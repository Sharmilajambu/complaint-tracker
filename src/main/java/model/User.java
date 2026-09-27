package model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String role; // STUDENT, ADMIN, or AUTHORITY
    private String designation;
    private String authorityCategory; // mess, hostel, ... or "all" for Principal
    private Integer authorityLevel;   // 0, 1, or 2
    private String email;
    private String phone;

    public User() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isAuthority() { return "AUTHORITY".equals(role); }
    public boolean isStudent() { return "STUDENT".equals(role); }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getAuthorityCategory() { return authorityCategory; }
    public void setAuthorityCategory(String authorityCategory) { this.authorityCategory = authorityCategory; }

    public Integer getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(Integer authorityLevel) { this.authorityLevel = authorityLevel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
