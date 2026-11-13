package com.grievancecampus.servlet;
import javax.servlet.ServletException;import javax.servlet.http.*;import java.io.IOException;import com.grievancecampus.dao.GrievanceDAO;import com.grievancecampus.model.User;
public class GrievanceDetailsServlet extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{User u=(User)r.getSession().getAttribute("user");if(!"STUDENT".equals(u.getRole())){p.sendError(403);return;}try{long id=Long.parseLong(r.getParameter("id"));java.util.Map<String,Object> grievance=new GrievanceDAO().details(u.getId(),id);if(grievance==null){p.sendError(404);return;}r.setAttribute("grievance",grievance);r.getRequestDispatcher("/student/grievance-details.jsp").forward(r,p);}catch(NumberFormatException e){p.sendError(400);}catch(java.sql.SQLException e){getServletContext().log("Grievance detail query failed",e);p.sendError(500);}}
}
