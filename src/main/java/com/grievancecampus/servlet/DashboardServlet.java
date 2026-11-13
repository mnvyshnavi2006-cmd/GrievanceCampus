package com.grievancecampus.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import com.grievancecampus.model.User;
import com.grievancecampus.dao.DashboardDAO;
import com.grievancecampus.dao.GrievanceDAO;
import com.grievancecampus.dao.AssignmentDAO;

public class DashboardServlet extends HttpServlet {
	protected void doGet(HttpServletRequest r, HttpServletResponse p) throws IOException, ServletException {
		User u = (User) r.getSession().getAttribute("user");
		String role = u.getRole() == null ? "" : u.getRole().toUpperCase();
		try { r.setAttribute("counts", new DashboardDAO().counts(u)); if ("STUDENT".equals(role)) r.setAttribute("grievances", new GrievanceDAO().forStudent(u.getId())); if ("STAFF".equals(role)||"TEACHER".equals(role)||"CLASS_TEACHER".equals(role)||"HOD".equals(role)) r.setAttribute("tasks", new AssignmentDAO().forStaff(u.getId())); if ("ADMIN".equals(role)) r.setAttribute("recentGrievances", new GrievanceDAO().forReview(u)); }
		catch (java.sql.SQLException e) { getServletContext().log("Dashboard statistics unavailable", e); r.setAttribute("dashboardError", true); }
		String page = role.equals("STUDENT") ? "student/dashboard.jsp"
				: role.equals("TEACHER") || role.equals("CLASS_TEACHER") ? "teacher/dashboard.jsp"
						: role.equals("HOD") ? "hod/dashboard.jsp"
								: role.equals("STAFF") ? "staff/dashboard.jsp"
								: role.equals("ADMIN") ? "admin/dashboard.jsp" : null;
		if (page == null) { p.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
		r.getRequestDispatcher("/" + page).forward(r, p);
	}
}
