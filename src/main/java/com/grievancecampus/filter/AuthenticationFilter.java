package com.grievancecampus.filter;
import javax.servlet.*;import javax.servlet.http.*;import java.io.IOException;import com.grievancecampus.model.User;
public class AuthenticationFilter implements Filter {
 public void doFilter(ServletRequest req,ServletResponse res,FilterChain chain)throws IOException,ServletException{
  HttpServletRequest r=(HttpServletRequest)req;HttpServletResponse p=(HttpServletResponse)res;HttpSession session=r.getSession(false);Object value=session==null?null:session.getAttribute("user");
  if(!(value instanceof User)){p.sendRedirect(r.getContextPath()+"/login.jsp");return;}
  User u=(User)value;String path=r.getRequestURI().substring(r.getContextPath().length());String role=u.getRole();boolean allowed=true;
  if(path.startsWith("/student/"))allowed="STUDENT".equals(role);
  else if(path.startsWith("/teacher/"))allowed="TEACHER".equals(role)||"CLASS_TEACHER".equals(role);
  else if(path.startsWith("/hod/"))allowed="HOD".equals(role);
  else if(path.startsWith("/staff/"))allowed="STAFF".equals(role);
  else if(path.startsWith("/admin/"))allowed="ADMIN".equals(role);
  if(!allowed){p.sendError(HttpServletResponse.SC_FORBIDDEN);return;}chain.doFilter(req,res);
 }
}
