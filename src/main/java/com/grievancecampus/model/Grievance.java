package com.grievancecampus.model;

import java.time.LocalDateTime;

public class Grievance {
	private long id, studentId;
	private String title, category, description, status;
	private LocalDateTime submittedAt;

	public long getId() {
		return id;
	}

	public void setId(long v) {
		id = v;
	}

	public long getStudentId() {
		return studentId;
	}

	public void setStudentId(long v) {
		studentId = v;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String v) {
		title = v;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String v) {
		category = v;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String v) {
		description = v;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String v) {
		status = v;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(LocalDateTime v) {
		submittedAt = v;
	}
}
