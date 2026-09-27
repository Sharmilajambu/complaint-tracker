package model;

/** Complaint plus fields needed for authority dashboard (student contact, routing). */
public class ComplaintView extends Complaint {
    private String level0Authority;
    private String assignedDesignation;
    private String studentEmail;
    private String studentPhone;
    private String categoryLabel;

    public String getLevel0Authority() { return level0Authority; }
    public void setLevel0Authority(String level0Authority) { this.level0Authority = level0Authority; }

    public String getAssignedDesignation() { return assignedDesignation; }
    public void setAssignedDesignation(String assignedDesignation) { this.assignedDesignation = assignedDesignation; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getStudentPhone() { return studentPhone; }
    public void setStudentPhone(String studentPhone) { this.studentPhone = studentPhone; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
}
