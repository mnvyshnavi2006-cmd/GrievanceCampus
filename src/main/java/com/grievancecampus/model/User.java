package com.grievancecampus.model;

public class User {
	private long id;
	private String name, email, role, department, section, jobTitle;

	public User(long id, String name, String email, String role, String department, String section) {
		this(id, name, email, role, department, section, null);
	}

	public User(long id, String name, String email, String role, String department, String section, String jobTitle) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.role = role;
		this.department = department;
		this.section = section;
		this.jobTitle = jobTitle;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public String getDepartment() {
		return department;
	}

	public String getSection() {
		return section;
	}

	public String getJobTitle() { return jobTitle; }
}
