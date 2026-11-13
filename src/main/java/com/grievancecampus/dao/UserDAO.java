package com.grievancecampus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.dao.DBConnection;
import com.grievancecampus.model.User;
import com.grievancecampus.util.PasswordUtil;

public class UserDAO {
    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT u.user_id,u.full_name,u.email,u.role,u.department,u.section,u.job_title,s.password AS account_password "
                + "FROM gc_users u JOIN gc_students s ON s.user_id=u.user_id WHERE u.email=? AND u.active=1 AND u.role='STUDENT' "
                + "UNION ALL SELECT u.user_id,u.full_name,u.email,u.role,u.department,u.section,u.job_title,t.password "
                + "FROM gc_users u JOIN gc_teachers t ON t.user_id=u.user_id WHERE u.email=? AND u.active=1 AND u.role IN ('TEACHER','CLASS_TEACHER') "
                + "UNION ALL SELECT u.user_id,u.full_name,u.email,u.role,u.department,u.section,u.job_title,h.password "
                + "FROM gc_users u JOIN gc_hods h ON h.user_id=u.user_id WHERE u.email=? AND u.active=1 AND u.role='HOD' "
                + "UNION ALL SELECT u.user_id,u.full_name,u.email,u.role,u.department,u.section,u.job_title,s.password "
                + "FROM gc_users u JOIN gc_staff s ON s.user_id=u.user_id WHERE u.email=? AND u.active=1 AND u.role='STAFF' "
                + "UNION ALL SELECT u.user_id,u.full_name,u.email,u.role,u.department,u.section,u.job_title,a.password "
                + "FROM gc_users u JOIN gc_admins a ON a.user_id=u.user_id WHERE u.email=? AND u.active=1 AND u.role='ADMIN'";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) p.setString(i, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next() && password != null && password.equals(r.getString("account_password"))) {
                    return userFrom(r);
                }
            }
        }
        return authenticateLegacyHashedAccount(email, password);
    }

    private User authenticateLegacyHashedAccount(String email, String password) throws SQLException {
        String sql = "SELECT user_id,full_name,email,role,department,section,job_title,password_hash FROM gc_users "
                + "WHERE email=? AND active=1 AND password_hash<>''";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    String stored = r.getString("password_hash");
                    if (PasswordUtil.verify(password, stored) || password != null && password.equals(stored)) return userFrom(r);
                }
            }
        }
        return null;
    }

    public List<User> list() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id,full_name,email,role,department,section,job_title FROM gc_users ORDER BY role,full_name";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next()) users.add(userFrom(r));
        }
        return users;
    }

    public List<User> listActiveStaff() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id,full_name,email,role,department,section,job_title FROM gc_users "
                + "WHERE role IN ('STAFF','TEACHER','CLASS_TEACHER','HOD') AND active=1 ORDER BY department,role,job_title,full_name";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next()) users.add(userFrom(r));
        }
        return users;
    }

    public List<User> listActiveCoordinators() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id,full_name,email,role,department,section,job_title FROM gc_users "
                + "WHERE role IN ('TEACHER','CLASS_TEACHER','HOD') AND active=1 "
                + "ORDER BY role,department,section,full_name";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql);
                ResultSet r = p.executeQuery()) {
            while (r.next()) users.add(userFrom(r));
        }
        return users;
    }

    public void create(String name, String email, String password, String role, String department, String section)
            throws SQLException {
        create(name, email, password, role, department, section, null);
    }

    public void create(String name, String email, String password, String role, String department, String section, String jobTitle)
            throws SQLException {
        String profileTable = profileTable(role);
        String insertUser = "INSERT INTO gc_users(full_name,email,password_hash,role,department,section,job_title,active) "
                + "VALUES(?,?, '', ?,?,?,?,TRUE)";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement user = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                user.setString(1, name);
                user.setString(2, email);
                user.setString(3, role);
                user.setString(4, department);
                user.setString(5, section);
                user.setString(6, jobTitle);
                user.executeUpdate();
                long userId;
                try (ResultSet keys = user.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Could not create the account ID.");
                    userId = keys.getLong(1);
                }
                insertProfile(c, profileTable, userId, password);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void registerStudent(String name, String email, String password, String department, String section)
            throws SQLException {
        create(name, email, password, "STUDENT", department, section);
    }

    private void insertProfile(Connection connection, String table, long userId, String password) throws SQLException {
        String sql = "INSERT INTO " + table + "(user_id,password) VALUES(?,?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setLong(1, userId);
            p.setString(2, password);
            p.executeUpdate();
        }
    }

    private String profileTable(String role) throws SQLException {
        switch (role) {
            case "STUDENT": return "gc_students";
            case "TEACHER":
            case "CLASS_TEACHER": return "gc_teachers";
            case "HOD": return "gc_hods";
            case "STAFF": return "gc_staff";
            case "ADMIN": return "gc_admins";
            default: throw new SQLException("Unsupported account role.");
        }
    }

    private User userFrom(ResultSet r) throws SQLException {
        return new User(r.getLong("user_id"), r.getString("full_name"), r.getString("email"),
                r.getString("role"), r.getString("department"), r.getString("section"), r.getString("job_title"));
    }
}
