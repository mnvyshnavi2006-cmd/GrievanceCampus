package com.grievancecampus.servlet;

import javax.servlet.http.*;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {
	protected void doGet(HttpServletRequest r, HttpServletResponse p) throws IOException {
		HttpSession s = r.getSession(false);
		if (s != null)
			s.invalidate();
		p.sendRedirect(r.getContextPath() + "/login.jsp");
	}
}
