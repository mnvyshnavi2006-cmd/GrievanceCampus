package com.grievancecampus.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.grievancecampus.dao.UserDAO;
import com.grievancecampus.util.ValidationUtil;

public class StudentRegistrationServlet extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String name = clean(request.getParameter("name"));
		String email = clean(request.getParameter("email")).toLowerCase(java.util.Locale.ROOT);
		String password = request.getParameter("password");
		String department = clean(request.getParameter("department"));
		String section = clean(request.getParameter("section"));

		if (name.isEmpty() || name.length() > 120 || !ValidationUtil.validEmail(email)
				|| password == null || password.length() < 8 || password.length() > 255
				|| department.isEmpty() || department.length() > 120 || section.isEmpty() || section.length() > 50) {
			response.sendRedirect(request.getContextPath() + "/register.jsp?error=validation");
			return;
		}

		try {
			new UserDAO().registerStudent(name, email, password, department, section);
			response.sendRedirect(request.getContextPath() + "/login.jsp?registered=1");
		} catch (SQLException e) {
			getServletContext().log("Student registration failed", e);
			String error = e.getErrorCode() == 1062 ? "duplicate" : "save";
			response.sendRedirect(request.getContextPath() + "/register.jsp?error=" + error);
		}
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}
}
