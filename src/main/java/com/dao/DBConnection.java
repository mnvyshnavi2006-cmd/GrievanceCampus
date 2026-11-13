package com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens connections and creates missing Grievance Campus tables on first use. */
public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/grievance_campus"
            + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static volatile boolean tablesReady;

    private static final String[] CREATE_TABLES = {
        "CREATE TABLE IF NOT EXISTS gc_users ("
            + "user_id BIGINT PRIMARY KEY AUTO_INCREMENT, full_name VARCHAR(120) NOT NULL, "
            + "email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL DEFAULT '', "
            + "role VARCHAR(20) NOT NULL, department VARCHAR(120), section VARCHAR(50), job_title VARCHAR(80), "
            + "active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "CHECK (role IN ('STUDENT','TEACHER','CLASS_TEACHER','HOD','STAFF','ADMIN'))) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_students (user_id BIGINT PRIMARY KEY, password VARCHAR(255) NOT NULL, "
            + "FOREIGN KEY (user_id) REFERENCES gc_users(user_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_teachers (user_id BIGINT PRIMARY KEY, password VARCHAR(255) NOT NULL, "
            + "FOREIGN KEY (user_id) REFERENCES gc_users(user_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_hods (user_id BIGINT PRIMARY KEY, password VARCHAR(255) NOT NULL, "
            + "FOREIGN KEY (user_id) REFERENCES gc_users(user_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_staff (user_id BIGINT PRIMARY KEY, password VARCHAR(255) NOT NULL, "
            + "FOREIGN KEY (user_id) REFERENCES gc_users(user_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_admins (user_id BIGINT PRIMARY KEY, password VARCHAR(255) NOT NULL, "
            + "FOREIGN KEY (user_id) REFERENCES gc_users(user_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_grievances ("
            + "grievance_id BIGINT PRIMARY KEY AUTO_INCREMENT, student_id BIGINT NOT NULL, "
            + "title VARCHAR(150) NOT NULL, category VARCHAR(40) NOT NULL, description TEXT NOT NULL, "
            + "department VARCHAR(120) NOT NULL, section VARCHAR(50), photo_path VARCHAR(255), "
            + "latitude DECIMAL(10,7), longitude DECIMAL(10,7), location_text VARCHAR(300), "
            + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMP NULL, "
            + "completion_notes TEXT, deleted_at TIMESTAMP NULL, CONSTRAINT fk_gc_grievance_student FOREIGN KEY (student_id) "
            + "REFERENCES gc_users(user_id), INDEX idx_gc_grievance_scope (department, section, status), "
            + "INDEX idx_gc_grievance_student (student_id)) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_assignments ("
            + "assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT, grievance_id BIGINT NOT NULL, "
            + "assignee_id BIGINT NOT NULL, assigned_by BIGINT NOT NULL, coordinator_id BIGINT NULL, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMP NULL, "
            + "completion_notes TEXT, FOREIGN KEY (grievance_id) REFERENCES gc_grievances(grievance_id), "
            + "FOREIGN KEY (assignee_id) REFERENCES gc_users(user_id), "
            + "FOREIGN KEY (assigned_by) REFERENCES gc_users(user_id), "
            + "FOREIGN KEY (coordinator_id) REFERENCES gc_users(user_id), "
            + "INDEX idx_gc_assignment_owner (assignee_id, status), "
            + "CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED'))) ENGINE=InnoDB",
        "CREATE TABLE IF NOT EXISTS gc_updates ("
            + "update_id BIGINT PRIMARY KEY AUTO_INCREMENT, assignment_id BIGINT NOT NULL, "
            + "author_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL, note TEXT NOT NULL, "
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (assignment_id) REFERENCES gc_assignments(assignment_id), "
            + "FOREIGN KEY (author_id) REFERENCES gc_users(user_id), "
            + "CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED'))) ENGINE=InnoDB"
    };

    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = setting("DB_URL", DEFAULT_URL);
            String username = setting("DB_USER", "root");
            String password = "teja2126";
            Connection connection = DriverManager.getConnection(url, username, password);
            try {
                ensureTables(connection);
                System.out.println("Database Connected Successfully");
                return connection;
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Check the Maven dependency.", e);
        }
    }

    private static void ensureTables(Connection connection) throws SQLException {
        if (tablesReady) return;
        synchronized (DBConnection.class) {
            if (tablesReady) return;
            try (Statement statement = connection.createStatement()) {
                for (String sql : CREATE_TABLES) statement.executeUpdate(sql);
            }
            ensureCoordinatorColumn(connection);
            ensureUserJobTitleColumn(connection);
            ensureGrievanceDeletedColumn(connection);
            normalizeLegacyProcessingStatuses(connection);
            createFirstAdmin(connection);
            tablesReady = true;
        }
    }

    private static void ensureCoordinatorColumn(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gc_assignments' AND COLUMN_NAME='coordinator_id'";
        try (Statement query = connection.createStatement(); java.sql.ResultSet result = query.executeQuery(sql)) {
            if (result.next() && result.getLong(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE gc_assignments ADD COLUMN coordinator_id BIGINT NULL");
                }
            }
        }
    }

    private static void ensureUserJobTitleColumn(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gc_users' AND COLUMN_NAME='job_title'";
        try (Statement query = connection.createStatement(); java.sql.ResultSet result = query.executeQuery(sql)) {
            if (result.next() && result.getLong(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE gc_users ADD COLUMN job_title VARCHAR(80) NULL");
                }
            }
        }
    }

    private static void ensureGrievanceDeletedColumn(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gc_grievances' AND COLUMN_NAME='deleted_at'";
        try (Statement query = connection.createStatement(); java.sql.ResultSet result = query.executeQuery(sql)) {
            if (result.next() && result.getLong(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE gc_grievances ADD COLUMN deleted_at TIMESTAMP NULL");
                }
            }
        }
    }

    private static void normalizeLegacyProcessingStatuses(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE gc_assignments SET status='PENDING' WHERE status='IN_PROGRESS'");
            statement.executeUpdate("UPDATE gc_grievances g SET status=CASE "
                    + "WHEN EXISTS (SELECT 1 FROM gc_assignments a WHERE a.grievance_id=g.grievance_id) "
                    + "AND NOT EXISTS (SELECT 1 FROM gc_assignments a WHERE a.grievance_id=g.grievance_id AND a.status<>'COMPLETED') "
                    + "THEN 'RESOLVED' ELSE 'PENDING' END WHERE g.status='IN_PROGRESS'");
        }
    }

    private static void createFirstAdmin(Connection connection) throws SQLException {
        String email = setting("GC_ADMIN_EMAIL", "").trim();
        String password = setting("GC_ADMIN_PASSWORD", "");
        if (email.isEmpty() || email.length() > 254 || !email.contains("@")
                || password.length() < 8 || password.length() > 255) return;

        try (Statement count = connection.createStatement();
                java.sql.ResultSet result = count.executeQuery("SELECT COUNT(*) FROM gc_users WHERE role='ADMIN'")) {
            if (!result.next() || result.getLong(1) != 0) return;
        }

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement user = connection.prepareStatement(
                "INSERT INTO gc_users(full_name,email,password_hash,role,active) VALUES('Campus Administrator',?, '', 'ADMIN',TRUE)",
                Statement.RETURN_GENERATED_KEYS)) {
            user.setString(1, email);
            user.executeUpdate();
            try (java.sql.ResultSet keys = user.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Could not create the initial admin account.");
                try (PreparedStatement profile = connection.prepareStatement(
                        "INSERT INTO gc_admins(user_id,password) VALUES(?,?)")) {
                    profile.setLong(1, keys.getLong(1));
                    profile.setString(2, password);
                    profile.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
