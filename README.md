# Grievance Campus

A small Java 11 Servlet/JSP project for submitting and tracking campus grievances. It uses JDBC, MySQL, vanilla JavaScript/CSS, and is packaged as a WAR for Tomcat 9.

## Features

- Five account roles: Student, Teacher, HOD, Staff, and Admin.
- Public student registration with email sign-in.
- Separate student, teacher, HOD, staff, and admin credential tables with session login/logout and role checked pages.
- Student grievance submission with category, department, section, optional GPS and image.
- Department and section scoped review queues; Admin, HOD, and Teachers can assign grievances to active staff in the matching department.
- Staff task updates are limited to the worker's own assignments. Pending, in-progress, and completed updates are recorded in history and update the grievance status shown on student and Admin dashboards.
- Completion notes and dashboard counts come from MySQL queries.
- Admin account creation and user listing.

## Database behavior

`com.dao.DBConnection` is the only Java database connection class. On the first connection it requests database creation and creates missing app tables with `CREATE TABLE IF NOT EXISTS`. `gc_users` stores the shared account ID, role, and profile fields. Credentials for new accounts are stored in separate `gc_students`, `gc_teachers`, `gc_hods`, `gc_staff`, and `gc_admins` tables. New passwords are stored as entered, per the project requirement. Older accounts with a BCrypt value in `gc_users.password_hash` can still sign in. The MySQL account needs permission to create the database and tables. If it lacks those permissions, run `database/migrations.sql` in Workbench. Existing tables and records are not dropped or truncated.

## Configure local credentials

Set database connection values as environment variables in the Eclipse Tomcat launch configuration before startup:

- `DB_URL` (optional; defaults to `jdbc:mysql://localhost:3306/grievance_campus?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`)
- `DB_USER` (optional; defaults to `root`)
- `DB_PASSWORD` (your local MySQL password; if your local root account has no password, leave it unset)
- Optional `GC_UPLOAD_DIR` for image storage outside the web root

Set `GC_ADMIN_EMAIL` and `GC_ADMIN_PASSWORD` before first startup to create the initial Admin account automatically. Account passwords for new accounts are plain text in their role-specific table, as requested; existing BCrypt and legacy plaintext accounts in `gc_users.password_hash` can use the compatibility login path.

## Import and run

1. In Eclipse, choose **File → Import → Existing Maven Projects** and select this folder.
2. Set the project JRE to JDK 11, update Maven dependencies, and configure an Apache Tomcat 9 server.
3. Start MySQL. The app creates its database and tables on first connection. If the MySQL account lacks database/table creation permissions, have a DBA run `database/migrations.sql` in Workbench.
4. Set `DB_PASSWORD` and, for a new installation, `GC_ADMIN_EMAIL` and `GC_ADMIN_PASSWORD` in the Eclipse Tomcat launch configuration. Run the project on Tomcat 9. For a standalone package use `mvn clean package`, then deploy `target/GrievanceCampus.war`.
5. Open `http://localhost:8080/GrievanceCampus/` and sign in with an existing account.

Students can create an account at `http://localhost:8080/GrievanceCampus/register.jsp`. Registration creates a `STUDENT` account in `gc_users`; the email address must be unique.

## Testing status and limitations

The Java sources compile with `javac --release 11` against the cached Servlet 4.0.1 and BCrypt jars. Maven packaging could not be run because Maven is not installed/on PATH. MySQL connection and end-to-end tests could not run because `localhost:3306` did not accept a connection. No production-schema compatibility is claimed. Images are restricted to JPG, PNG, and WebP signatures and served through an authenticated servlet. Assign staff by entering the account ID shown in Admin → Manage users.
