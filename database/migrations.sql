-- Additive and idempotent setup. It only creates missing app tables; it does not modify campus tables.
CREATE DATABASE IF NOT EXISTS grievance_campus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE grievance_campus;

CREATE TABLE IF NOT EXISTS gc_users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE,
  -- Kept for backward compatibility with accounts created by the earlier version.
  -- New account passwords are stored in the matching role table below.
  password_hash VARCHAR(100) NOT NULL DEFAULT '',
  role VARCHAR(20) NOT NULL,
  department VARCHAR(120),
  section VARCHAR(50),
  job_title VARCHAR(80),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CHECK (role IN ('STUDENT','TEACHER','CLASS_TEACHER','HOD','STAFF','ADMIN'))
) ENGINE=InnoDB;

SET @gc_has_job_title = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gc_users' AND COLUMN_NAME='job_title');
SET @gc_job_title_sql = IF(@gc_has_job_title=0, 'ALTER TABLE gc_users ADD COLUMN job_title VARCHAR(80) NULL', 'SELECT 1');
PREPARE gc_job_title_stmt FROM @gc_job_title_sql;
EXECUTE gc_job_title_stmt;
DEALLOCATE PREPARE gc_job_title_stmt;

CREATE TABLE IF NOT EXISTS gc_students (
  user_id BIGINT PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES gc_users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_teachers (
  user_id BIGINT PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES gc_users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_hods (
  user_id BIGINT PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES gc_users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_staff (
  user_id BIGINT PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES gc_users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_admins (
  user_id BIGINT PRIMARY KEY,
  password VARCHAR(255) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES gc_users(user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_grievances (
  grievance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  category VARCHAR(40) NOT NULL,
  description TEXT NOT NULL,
  department VARCHAR(120) NOT NULL,
  section VARCHAR(50),
  photo_path VARCHAR(255),
  latitude DECIMAL(10,7),
  longitude DECIMAL(10,7),
  location_text VARCHAR(300),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  completion_notes TEXT,
  CONSTRAINT fk_gc_grievance_student FOREIGN KEY (student_id) REFERENCES gc_users(user_id),
  INDEX idx_gc_grievance_scope (department, section, status),
  INDEX idx_gc_grievance_student (student_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gc_assignments (
  assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  grievance_id BIGINT NOT NULL,
  assignee_id BIGINT NOT NULL,
  assigned_by BIGINT NOT NULL,
  coordinator_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  completion_notes TEXT,
  FOREIGN KEY (grievance_id) REFERENCES gc_grievances(grievance_id),
  FOREIGN KEY (assignee_id) REFERENCES gc_users(user_id),
  FOREIGN KEY (assigned_by) REFERENCES gc_users(user_id),
  FOREIGN KEY (coordinator_id) REFERENCES gc_users(user_id),
  INDEX idx_gc_assignment_owner (assignee_id, status),
  CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED'))
) ENGINE=InnoDB;

SET @gc_has_coordinator = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gc_assignments' AND COLUMN_NAME='coordinator_id');
SET @gc_coordinator_sql = IF(@gc_has_coordinator=0, 'ALTER TABLE gc_assignments ADD COLUMN coordinator_id BIGINT NULL', 'SELECT 1');
PREPARE gc_coordinator_stmt FROM @gc_coordinator_sql;
EXECUTE gc_coordinator_stmt;
DEALLOCATE PREPARE gc_coordinator_stmt;
CREATE TABLE IF NOT EXISTS gc_updates (
  update_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  note TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (assignment_id) REFERENCES gc_assignments(assignment_id),
  FOREIGN KEY (author_id) REFERENCES gc_users(user_id),
  CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED'))
) ENGINE=InnoDB;
