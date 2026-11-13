package com.grievancecampus.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.grievancecampus.dao.AssignmentDAO;
import com.grievancecampus.model.User;

public class AssignmentServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException,javax.servlet.ServletException{
        User user=(User)request.getSession().getAttribute("user");if(!isWorker(user)){response.sendError(403);return;}try{AssignmentDAO dao=new AssignmentDAO();String taskId=request.getParameter("taskId");if(taskId!=null){long id=Long.parseLong(taskId);java.util.Map<String,Object> task=dao.detailsForWorker(user.getId(),id);if(task==null){response.sendError(404);return;}request.setAttribute("task",task);request.getRequestDispatcher("/staff/task-details.jsp").forward(request,response);return;}request.setAttribute("tasks",dao.forStaff(user.getId()));request.getRequestDispatcher("/staff/dashboard.jsp").forward(request,response);}catch(NumberFormatException e){response.sendError(400);}catch(java.sql.SQLException e){getServletContext().log("Worker task list unavailable",e);response.sendError(500);}
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException{
        User user=(User)request.getSession().getAttribute("user");String action=request.getParameter("action");
        String returnPath="update".equals(action)?"/app/dashboard":"/app/grievances";
        try{
            AssignmentDAO dao=new AssignmentDAO();
            if("assign".equals(action)&&is(user,"ADMIN")){String coordinator=request.getParameter("coordinatorId");Long coordinatorId=coordinator==null||coordinator.trim().isEmpty()?null:Long.valueOf(coordinator);dao.assign(Long.parseLong(request.getParameter("grievanceId")),Long.parseLong(request.getParameter("assigneeId")),coordinatorId,user);}
            else if("reject".equals(action)&&is(user,"ADMIN")){dao.reject(Long.parseLong(request.getParameter("grievanceId")),user);}
            else if("update".equals(action)&&isWorker(user)){String status=request.getParameter("status");if(!"PENDING".equals(status)&&!"COMPLETED".equals(status))throw new IllegalArgumentException("Choose Pending or Completed.");String note=request.getParameter("note");if(note==null||note.trim().isEmpty()||note.length()>3000)throw new IllegalArgumentException("Enter a progress note up to 3000 characters.");dao.update(Long.parseLong(request.getParameter("assignmentId")),user.getId(),status,note.trim());}
            else {response.sendError(403);return;}
            response.sendRedirect(request.getContextPath()+returnPath+"?updated=1");
        }catch(IllegalArgumentException e){response.sendRedirect(request.getContextPath()+returnPath+"?error=invalid");}catch(java.sql.SQLException e){getServletContext().log("Assignment action failed",e);response.sendRedirect(request.getContextPath()+returnPath+"?error=save");}
    }
    private boolean is(User user,String... roles){if(user==null)return false;for(String role:roles)if(role.equals(user.getRole()))return true;return false;}
    private boolean isWorker(User user){return is(user,"STAFF","TEACHER","CLASS_TEACHER","HOD");}
}
