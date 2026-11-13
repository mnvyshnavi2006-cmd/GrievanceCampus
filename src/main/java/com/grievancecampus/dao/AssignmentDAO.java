package com.grievancecampus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.dao.DBConnection;
import com.grievancecampus.model.User;

public class AssignmentDAO {
    public void assign(long grievanceId, long workerId, Long coordinatorId, User actor) throws SQLException {
        if (actor == null || !"ADMIN".equals(actor.getRole())) throw new SQLException("Only administrators can assign grievances.");
        String eligible = "SELECT COUNT(*) FROM gc_grievances g "
                + "JOIN gc_users worker ON worker.user_id=? AND worker.role IN ('STAFF','TEACHER','CLASS_TEACHER','HOD') AND worker.active=1 "
                + "LEFT JOIN gc_users coordinator ON coordinator.user_id=? AND coordinator.active=1 "
                + "AND coordinator.role IN ('TEACHER','CLASS_TEACHER','HOD') "
                // A rejected, unassigned request can be reopened by Admin by assigning it.
                + "WHERE g.grievance_id=? AND g.deleted_at IS NULL AND g.status<>'RESOLVED' "
                + "AND (?='ADMIN' OR worker.department=g.department) "
                + "AND (? IS NULL OR (coordinator.user_id IS NOT NULL AND coordinator.department=g.department "
                + "AND (coordinator.role='HOD' OR coordinator.section=g.section))) "
                + "AND (?='ADMIN' OR (?='HOD' AND g.department=?) "
                + "OR (? IN ('TEACHER','CLASS_TEACHER') AND g.department=? AND g.section=?))";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(eligible)) {
                    p.setLong(1, workerId);
                    if (coordinatorId == null) p.setNull(2, java.sql.Types.BIGINT); else p.setLong(2, coordinatorId);
                    p.setLong(3, grievanceId);
                    p.setString(4, actor.getRole());
                    if (coordinatorId == null) p.setNull(5, java.sql.Types.BIGINT); else p.setLong(5, coordinatorId);
                    p.setString(6, actor.getRole());
                    p.setString(7, actor.getRole());
                    p.setString(8, actor.getDepartment());
                    p.setString(9, actor.getRole());
                    p.setString(10, actor.getDepartment());
                    p.setString(11, actor.getSection());
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next() || r.getInt(1) != 1) throw new SQLException("Assignment denied: verify the grievance scope, worker, and coordinator.");
                    }
                }
                long assignmentId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO gc_assignments(grievance_id,assignee_id,assigned_by,coordinator_id) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1, grievanceId);
                    p.setLong(2, workerId);
                    p.setLong(3, actor.getId());
                    if (coordinatorId == null) p.setNull(4, java.sql.Types.BIGINT); else p.setLong(4, coordinatorId);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Assignment was created without an identifier.");
                        assignmentId = keys.getLong(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO gc_updates(assignment_id,author_id,status,note) VALUES(?,?,'PENDING',?)")) {
                    p.setLong(1, assignmentId);
                    p.setLong(2, actor.getId());
                    p.setString(3, "Assignment created for worker account #" + workerId
                            + (coordinatorId == null ? "" : " through coordinator account #" + coordinatorId) + ".");
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE gc_grievances g SET status='PENDING', completed_at=NULL, completion_notes=NULL WHERE g.grievance_id=?")) {
                    p.setLong(1, grievanceId);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void reject(long grievanceId, User actor) throws SQLException {
        if (actor == null || !"ADMIN".equals(actor.getRole())) throw new SQLException("Only administrators can reject grievances.");
        String sql = "UPDATE gc_grievances g SET status='REJECTED' WHERE g.grievance_id=? AND g.deleted_at IS NULL AND g.status='PENDING' "
                + "AND NOT EXISTS(SELECT 1 FROM gc_assignments a WHERE a.grievance_id=g.grievance_id) "
                + "AND (?='ADMIN' OR (?='HOD' AND g.department=?) "
                + "OR (? IN ('TEACHER','CLASS_TEACHER') AND g.department=? AND g.section=?))";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, grievanceId);
            p.setString(2, actor.getRole());
            p.setString(3, actor.getRole());
            p.setString(4, actor.getDepartment());
            p.setString(5, actor.getRole());
            p.setString(6, actor.getDepartment());
            p.setString(7, actor.getSection());
            if (p.executeUpdate() != 1) throw new SQLException("Only an unassigned pending grievance can be rejected.");
        }
    }

    public void update(long assignmentId, long staffId, String status, String note) throws SQLException {
        if (!"PENDING".equals(status) && !"COMPLETED".equals(status)) {
            throw new SQLException("Unsupported task status.");
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long grievanceId;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT a.grievance_id FROM gc_assignments a JOIN gc_grievances g ON g.grievance_id=a.grievance_id WHERE a.assignment_id=? AND a.assignee_id=? AND g.deleted_at IS NULL FOR UPDATE")) {
                    p.setLong(1, assignmentId);
                    p.setLong(2, staffId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Task not found or it is assigned to another worker.");
                        grievanceId = r.getLong(1);
                    }
                }

                String updateTask = "UPDATE gc_assignments SET status=?, "
                        + "completed_at=CASE WHEN ?='COMPLETED' THEN CURRENT_TIMESTAMP ELSE NULL END, "
                        + "completion_notes=CASE WHEN ?='COMPLETED' THEN ? ELSE NULL END "
                        + "WHERE assignment_id=? AND assignee_id=?";
                try (PreparedStatement p = c.prepareStatement(updateTask)) {
                    p.setString(1, status);
                    p.setString(2, status);
                    p.setString(3, status);
                    p.setString(4, note);
                    p.setLong(5, assignmentId);
                    p.setLong(6, staffId);
                    if (p.executeUpdate() != 1) throw new SQLException("Task update did not complete.");
                }

                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO gc_updates(assignment_id,author_id,status,note) VALUES(?,?,?,?)")) {
                    p.setLong(1, assignmentId);
                    p.setLong(2, staffId);
                    p.setString(3, status);
                    p.setString(4, note);
                    p.executeUpdate();
                }

                String updateGrievance = "UPDATE gc_grievances g SET "
                        + "status=CASE "
                        + "WHEN NOT EXISTS (SELECT 1 FROM gc_assignments a WHERE a.grievance_id=g.grievance_id AND a.status<>'COMPLETED') THEN 'RESOLVED' "
                        + "ELSE 'PENDING' END, "
                        + "completed_at=CASE WHEN NOT EXISTS (SELECT 1 FROM gc_assignments a "
                        + "WHERE a.grievance_id=g.grievance_id AND a.status<>'COMPLETED') THEN CURRENT_TIMESTAMP ELSE NULL END, "
                        + "completion_notes=CASE WHEN NOT EXISTS (SELECT 1 FROM gc_assignments a "
                        + "WHERE a.grievance_id=g.grievance_id AND a.status<>'COMPLETED') THEN ? ELSE NULL END "
                        + "WHERE g.grievance_id=?";
                try (PreparedStatement p = c.prepareStatement(updateGrievance)) {
                    p.setString(1, note);
                    p.setLong(2, grievanceId);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public long countForUser(long userId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM gc_assignments WHERE assignee_id=?" + (status == null ? "" : " AND status=?");
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId);
            if (status != null) p.setString(2, status);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }

    public List<Map<String, Object>> forStaff(long staffId) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT a.assignment_id,a.status AS task_status,a.created_at,a.completion_notes, "
                + "g.grievance_id,g.title,g.category,g.description,g.department,g.section,g.status AS grievance_status,g.location_text,g.photo_path,g.created_at AS submitted_at, "
                + "assigner.full_name AS assigned_by_name, coordinator.full_name AS coordinator_name FROM gc_assignments a "
                + "JOIN gc_grievances g ON g.grievance_id=a.grievance_id "
                + "JOIN gc_users assigner ON assigner.user_id=a.assigned_by "
                + "LEFT JOIN gc_users coordinator ON coordinator.user_id=a.coordinator_id "
                + "WHERE a.assignee_id=? AND g.deleted_at IS NULL ORDER BY CASE a.status WHEN 'PENDING' THEN 0 ELSE 1 END, a.created_at DESC";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, staffId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String col : new String[] {"assignment_id", "task_status", "created_at", "completion_notes",
                            "grievance_id", "title", "category", "description", "department", "section",
                            "grievance_status", "location_text", "photo_path", "submitted_at", "assigned_by_name", "coordinator_name"}) row.put(col, r.getObject(col));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public Map<String, Object> detailsForWorker(long workerId, long assignmentId) throws SQLException {
        String sql = "SELECT a.assignment_id,a.status AS task_status,a.created_at AS assigned_at,a.completed_at AS task_completed_at,a.completion_notes, "
                + "g.grievance_id,g.title,g.category,g.description,g.department,g.section,g.status AS grievance_status,g.location_text,g.latitude,g.longitude,g.photo_path,g.created_at AS submitted_at,g.completed_at,g.completion_notes AS grievance_completion_notes, "
                + "assigner.full_name AS assigned_by_name,coordinator.full_name AS coordinator_name "
                + "FROM gc_assignments a JOIN gc_grievances g ON g.grievance_id=a.grievance_id "
                + "JOIN gc_users assigner ON assigner.user_id=a.assigned_by LEFT JOIN gc_users coordinator ON coordinator.user_id=a.coordinator_id "
                + "WHERE a.assignment_id=? AND a.assignee_id=? AND g.deleted_at IS NULL";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, assignmentId);
            p.setLong(2, workerId);
            Map<String, Object> task = new LinkedHashMap<>();
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                for (String col : new String[] {"assignment_id", "task_status", "assigned_at", "task_completed_at", "completion_notes",
                        "grievance_id", "title", "category", "description", "department", "section", "grievance_status",
                        "location_text", "latitude", "longitude", "photo_path", "submitted_at", "completed_at",
                        "grievance_completion_notes", "assigned_by_name", "coordinator_name"}) task.put(col, r.getObject(col));
            }
            List<Map<String, Object>> history = new ArrayList<>();
            try (PreparedStatement updates = c.prepareStatement(
                    "SELECT u.status,u.note,u.created_at,author.full_name AS author_name FROM gc_updates u "
                    + "JOIN gc_users author ON author.user_id=u.author_id WHERE u.assignment_id=? ORDER BY u.created_at,u.update_id")) {
                updates.setLong(1, assignmentId);
                try (ResultSet r = updates.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : new String[] {"status", "note", "created_at", "author_name"}) row.put(col, r.getObject(col));
                        history.add(row);
                    }
                }
            }
            task.put("updates", history);
            return task;
        }
    }
}
