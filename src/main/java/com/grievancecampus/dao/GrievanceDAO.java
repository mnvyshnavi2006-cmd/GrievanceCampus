package com.grievancecampus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.dao.DBConnection;
import com.grievancecampus.model.Grievance;
import com.grievancecampus.model.User;

public class GrievanceDAO {
    public long create(long studentId,String title,String category,String description,String department,String section,String photo,Double latitude,Double longitude,String location) throws SQLException {
        String sql="INSERT INTO gc_grievances(student_id,title,category,description,department,section,photo_path,latitude,longitude,location_text) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS)){
            p.setLong(1,studentId);p.setString(2,title);p.setString(3,category);p.setString(4,description);p.setString(5,department);p.setString(6,section);p.setString(7,photo);if(latitude==null)p.setNull(8,java.sql.Types.DECIMAL);else p.setDouble(8,latitude);if(longitude==null)p.setNull(9,java.sql.Types.DECIMAL);else p.setDouble(9,longitude);p.setString(10,location);p.executeUpdate();
            try(ResultSet keys=p.getGeneratedKeys()){if(keys.next())return keys.getLong(1);throw new SQLException("Grievance was saved without an identifier.");}
        }
    }
    public List<Grievance> forStudent(long studentId)throws SQLException{
        List<Grievance> rows=new ArrayList<>();String sql="SELECT grievance_id,student_id,title,category,description,status,created_at FROM gc_grievances WHERE student_id=? AND deleted_at IS NULL ORDER BY created_at DESC";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,studentId);try(ResultSet r=p.executeQuery()){while(r.next()){Grievance g=new Grievance();g.setId(r.getLong("grievance_id"));g.setStudentId(r.getLong("student_id"));g.setTitle(r.getString("title"));g.setCategory(r.getString("category"));g.setDescription(r.getString("description"));g.setStatus(r.getString("status"));g.setSubmittedAt(r.getTimestamp("created_at").toLocalDateTime());rows.add(g);}}}return rows;
    }
    public long count(long studentId,String status)throws SQLException{
        String sql="SELECT COUNT(*) FROM gc_grievances WHERE student_id=? AND deleted_at IS NULL"+(status==null?"":" AND status=?");
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,studentId);if(status!=null)p.setString(2,status);try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}}
    }

    public List<java.util.Map<String,Object>> forReview(com.grievancecampus.model.User user)throws SQLException{
        List<java.util.Map<String,Object>> rows=new ArrayList<>();String sql="SELECT g.grievance_id,g.title,g.category,g.description,g.department,g.section,g.status,g.created_at,g.location_text,g.latitude,g.longitude,g.photo_path,s.user_id AS student_id,s.full_name AS student_name,s.email AS student_email,COALESCE((SELECT GROUP_CONCAT(CONCAT(worker.full_name,' · ',a.status,IF(coordinator.full_name IS NULL,'',CONCAT(' · via ',coordinator.full_name))) ORDER BY a.created_at SEPARATOR ', ') FROM gc_assignments a JOIN gc_users worker ON worker.user_id=a.assignee_id LEFT JOIN gc_users coordinator ON coordinator.user_id=a.coordinator_id WHERE a.grievance_id=g.grievance_id),'Not assigned') AS assignment_summary FROM gc_grievances g JOIN gc_users s ON s.user_id=g.student_id WHERE g.deleted_at IS NULL";
        if("HOD".equals(user.getRole()))sql+=" AND g.department=?";else if("TEACHER".equals(user.getRole())||"CLASS_TEACHER".equals(user.getRole()))sql+=" AND g.department=? AND g.section=?";sql+=" ORDER BY g.created_at DESC";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){int i=1;if("HOD".equals(user.getRole())||"TEACHER".equals(user.getRole())||"CLASS_TEACHER".equals(user.getRole()))p.setString(i++,user.getDepartment());if("TEACHER".equals(user.getRole())||"CLASS_TEACHER".equals(user.getRole()))p.setString(i,user.getSection());try(ResultSet r=p.executeQuery()){while(r.next()){java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();for(String col:new String[]{"grievance_id","title","category","description","department","section","status","created_at","location_text","latitude","longitude","photo_path","student_id","student_name","student_email","assignment_summary"})row.put(col,r.getObject(col));rows.add(row);}}}return rows;
    }
    public java.util.Map<String,Object> details(long studentId,long grievanceId)throws SQLException{
        String sql="SELECT grievance_id,title,category,description,department,section,status,created_at,completed_at,completion_notes,photo_path,latitude,longitude,location_text FROM gc_grievances WHERE grievance_id=? AND student_id=? AND deleted_at IS NULL";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,grievanceId);p.setLong(2,studentId);java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();
            try(ResultSet r=p.executeQuery()){if(!r.next())return null;for(String col:new String[]{"grievance_id","title","category","description","department","section","status","created_at","completed_at","completion_notes","photo_path","latitude","longitude","location_text"})data.put(col,r.getObject(col));}
            data.put("updates",updates(c,grievanceId));return data;
        }
    }
    private List<java.util.Map<String,Object>> updates(Connection c,long grievanceId)throws SQLException{
        String sql="SELECT u.status,u.note,u.created_at,author.full_name AS author_name FROM gc_updates u JOIN gc_assignments a ON a.assignment_id=u.assignment_id JOIN gc_users author ON author.user_id=u.author_id WHERE a.grievance_id=? ORDER BY u.created_at,u.update_id";List<java.util.Map<String,Object>> list=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,grievanceId);try(ResultSet r=p.executeQuery()){while(r.next()){java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();row.put("status",r.getString("status"));row.put("note",r.getString("note"));row.put("created_at",r.getTimestamp("created_at"));row.put("author_name",r.getString("author_name"));list.add(row);}}}return list;
    }

    /** Soft-deletes a grievance so assignment and progress history remain available in the database. */
    public boolean delete(long grievanceId, User actor) throws SQLException {
        if (actor == null || !("ADMIN".equals(actor.getRole()) || "STUDENT".equals(actor.getRole()))) {
            throw new SQLException("Only the student owner or an administrator can delete a grievance.");
        }
        String sql = "UPDATE gc_grievances SET deleted_at=CURRENT_TIMESTAMP WHERE grievance_id=? "
                + "AND deleted_at IS NULL AND (?='ADMIN' OR ( ?='STUDENT' AND student_id=?))";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, grievanceId);
            p.setString(2, actor.getRole());
            p.setString(3, actor.getRole());
            p.setLong(4, actor.getId());
            return p.executeUpdate() == 1;
        }
    }
}
