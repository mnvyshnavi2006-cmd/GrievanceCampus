package com.grievancecampus.servlet;

import javax.servlet.http.*;import javax.servlet.annotation.MultipartConfig;import javax.servlet.ServletException;import java.io.IOException;import com.grievancecampus.dao.GrievanceDAO;import com.grievancecampus.dao.UserDAO;import com.grievancecampus.model.User;

@MultipartConfig(maxFileSize=5*1024*1024, maxRequestSize=6*1024*1024)
public class GrievanceServlet extends HttpServlet {
	protected void doGet(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{User u=(User)r.getSession().getAttribute("user");if(!java.util.Arrays.asList("ADMIN","HOD","TEACHER","CLASS_TEACHER").contains(u.getRole())){p.sendError(403);return;}try{r.setAttribute("queue",new GrievanceDAO().forReview(u));r.setAttribute("staffUsers",new UserDAO().listActiveStaff());r.setAttribute("coordinatorUsers",new UserDAO().listActiveCoordinators());r.getRequestDispatcher("/admin/grievances.jsp").forward(r,p);}catch(java.sql.SQLException e){getServletContext().log("Grievance queue unavailable",e);p.sendError(500);}}
	protected void doPost(HttpServletRequest r,HttpServletResponse p)throws IOException,ServletException{
		User u=(User)r.getSession().getAttribute("user");String action=clean(r.getParameter("action"));
		if("delete".equals(action)){
			if(u==null||!("STUDENT".equals(u.getRole())||"ADMIN".equals(u.getRole()))){p.sendError(403);return;}
			try{boolean deleted=new GrievanceDAO().delete(Long.parseLong(r.getParameter("grievanceId")),u);String target="ADMIN".equals(u.getRole())?"/app/grievances":"/app/dashboard";p.sendRedirect(r.getContextPath()+target+(deleted?"?deleted=1":"?error=delete"));}
			catch(NumberFormatException e){p.sendError(400);}catch(java.sql.SQLException e){getServletContext().log("Grievance deletion failed",e);p.sendRedirect(r.getContextPath()+("ADMIN".equals(u.getRole())?"/app/grievances":"/app/dashboard")+"?error=delete");}return;
		}
		if(u==null||!"STUDENT".equals(u.getRole())){p.sendError(403);return;}
		String title=clean(r.getParameter("title")),category=clean(r.getParameter("category")),description=clean(r.getParameter("description")),department=clean(r.getParameter("department")),section=clean(r.getParameter("section")),location=clean(r.getParameter("location"));
		if(u.getDepartment()!=null)department=u.getDepartment();if(u.getSection()!=null)section=u.getSection();
		if(title.isEmpty()||title.length()>150||description.isEmpty()||description.length()>5000||department.isEmpty()||department.length()>120||section.length()>50||location.isEmpty()||location.length()>300||!java.util.Arrays.asList("Academic","Cleaning","Discipline","Fees","Lab","Sports","Infrastructure","Other").contains(category)){p.sendRedirect(r.getContextPath()+"/student/submit-grievance.jsp?error=validation");return;}
		java.nio.file.Path saved=null;
		try{
			javax.servlet.http.Part photo=r.getPart("photo");String photoPath=null;if(photo!=null&&photo.getSize()>0){photoPath=savePhoto(photo);saved=uploadDirectory().resolve(photoPath);}
			String latitude=clean(r.getParameter("latitude")),longitude=clean(r.getParameter("longitude"));
			if(!latitude.isEmpty()&&!com.grievancecampus.util.ValidationUtil.validCoordinate(latitude,-90,90)||!longitude.isEmpty()&&!com.grievancecampus.util.ValidationUtil.validCoordinate(longitude,-180,180))throw new IllegalArgumentException();
			Double lat=latitude.isEmpty()?null:Double.valueOf(latitude),lon=longitude.isEmpty()?null:Double.valueOf(longitude);
			new GrievanceDAO().create(u.getId(),title,category,description,department,section,photoPath,lat,lon,location.isEmpty()?null:location);p.sendRedirect(r.getContextPath()+"/app/dashboard?saved=1");
		}catch(IllegalArgumentException e){if(saved!=null)java.nio.file.Files.deleteIfExists(saved);p.sendRedirect(r.getContextPath()+"/student/submit-grievance.jsp?error=validation");}
		catch(java.sql.SQLException|IOException|ServletException e){if(saved!=null)java.nio.file.Files.deleteIfExists(saved);getServletContext().log("Grievance submission failed",e);p.sendRedirect(r.getContextPath()+"/student/submit-grievance.jsp?error=save");}
	}
	private String clean(String value){return value==null?"":value.trim();}
	private java.nio.file.Path uploadDirectory()throws IOException{String configured=System.getenv("GC_UPLOAD_DIR");java.nio.file.Path dir=configured==null||configured.isBlank()?java.nio.file.Paths.get(System.getProperty("catalina.base","."),"grievance-uploads"):java.nio.file.Paths.get(configured);java.nio.file.Files.createDirectories(dir);return dir.toRealPath();}
	private String savePhoto(javax.servlet.http.Part part)throws IOException{
		if(part.getSize()>5L*1024*1024)throw new IllegalArgumentException();String type=part.getContentType();byte[] head=new byte[12];
		try(java.io.InputStream in=part.getInputStream()){int n=in.read(head);if(n<12)throw new IllegalArgumentException();boolean jpg=(head[0]&255)==255&&(head[1]&255)==216&&(head[2]&255)==255;boolean png=(head[0]&255)==137&&head[1]==80&&head[2]==78&&head[3]==71;boolean webp=head[0]=='R'&&head[1]=='I'&&head[2]=='F'&&head[3]=='F'&&head[8]=='W'&&head[9]=='E'&&head[10]=='B'&&head[11]=='P';if(!(jpg&&"image/jpeg".equalsIgnoreCase(type)||png&&"image/png".equalsIgnoreCase(type)||webp&&"image/webp".equalsIgnoreCase(type)))throw new IllegalArgumentException();}
		String ext="image/jpeg".equalsIgnoreCase(type)?".jpg":"image/png".equalsIgnoreCase(type)?".png":".webp";String name=java.util.UUID.randomUUID()+ext;java.nio.file.Path dir=uploadDirectory(),dest=dir.resolve(name).normalize();if(!dest.getParent().equals(dir))throw new IllegalArgumentException();try(java.io.InputStream in=part.getInputStream()){java.nio.file.Files.copy(in,dest);}return name;
	}
}
