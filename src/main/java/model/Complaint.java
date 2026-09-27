package model;

import java.time.LocalDateTime;

public class Complaint {
    private int id;
    private String studentName;
    private String category;
    private String description;
    private LocalDateTime submittedDate;
    private LocalDateTime deadline;
    private String status;          // OPEN, ESCALATED, RESOLVED
    private int priorityScore;
    private String priorityTier;    // HIGH, MEDIUM, LOW
    private int escalationLevel;
    private String assignedDesignation;
    private String level0Designation;

    public Complaint() {}

    // ---- getters and setters ----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public String getPriorityTier() { return priorityTier; }
    public void setPriorityTier(String priorityTier) { this.priorityTier = priorityTier; }

    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }

    public String getAssignedDesignation() { return assignedDesignation; }
    public void setAssignedDesignation(String assignedDesignation) { this.assignedDesignation = assignedDesignation; }

    public String getLevel0Designation() { return level0Designation; }
    public void setLevel0Designation(String level0Designation) { this.level0Designation = level0Designation; }
}
