package com.grievancecampus.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import com.grievancecampus.dao.UserDAO;
import com.grievancecampus.model.User;

public class LoginServlet extends HttpServlet {
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		String email = req.getParameter("email"), pass = req.getParameter("password");
		try {
			User u = new UserDAO().authenticate(email == null ? "" : email.trim(), pass == null ? "" : pass);
			if (u == null) {
				res.sendRedirect(req.getContextPath() + "/login.jsp?error=credentials");
				return;
			}
			HttpSession old = req.getSession(false);
			if (old != null)
				old.invalidate();
			HttpSession s = req.getSession(true);
			s.setAttribute("user", u);
			s.setAttribute("csrf", java.util.UUID.randomUUID().toString());
			res.sendRedirect(req.getContextPath() + "/app/dashboard");
		} catch (java.sql.SQLException e) {
			getServletContext().log("Login failed", e);
			res.sendRedirect(req.getContextPath() + "/login.jsp?error=unavailable");
		}
	}
}
