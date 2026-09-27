package db;

import model.Complaint;
import model.ComplaintView;
import model.User;
import service.AuthorityMapper;
import service.PriorityScorer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {

    public Complaint createComplaint(int userId, String studentName, String category, String description) throws SQLException {
        int recentSimilar = countRecentSimilar(category, description);
        int score = PriorityScorer.computeScore(category, description, recentSimilar);
        String tier = PriorityScorer.computeTier(score);
        int deadlineHours = PriorityScorer.deadlineHoursForTier(tier);

        String level0 = AuthorityMapper.getDesignation(category, 0);
        String assigned = level0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(deadlineHours);

        String sql = "INSERT INTO complaints (user_id, student_name, category, description, submitted_date, deadline, status, priority_score, priority_tier, escalation_level, assigned_designation, level0_designation) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, 0, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, studentName);
            ps.setString(3, category);
            ps.setString(4, description);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(deadline));
            ps.setInt(7, score);
            ps.setString(8, tier);
            ps.setString(9, assigned);
            ps.setString(10, level0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getComplaintById(keys.getInt(1));
                }
            }
        }
        return null;
    }

    private int countRecentSimilar(String category, String description) throws SQLException {
        String sql = "SELECT description FROM complaints WHERE category = ? AND submitted_date >= (NOW() - INTERVAL 7 DAY)";
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (PriorityScorer.isSimilar(description, rs.getString("description"))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public List<Complaint> getComplaintsByUserId(int userId) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE user_id = ? ORDER BY submitted_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<ComplaintView> getComplaintsForAuthority(User authority) throws SQLException {
        List<ComplaintView> list = new ArrayList<>();
        String sql = """
            SELECT c.*, u.email AS student_email, u.phone AS student_phone
            FROM complaints c
            LEFT JOIN users u ON c.user_id = u.id
            WHERE c.status <> 'RESOLVED'
            """;

        boolean isPrincipal = authority.getAuthorityLevel() != null && authority.getAuthorityLevel() == 2;
        if (isPrincipal) {
            sql += " AND c.escalation_level = 2 AND c.assigned_designation = 'Principal'";
        } else {
            sql += " AND c.category = ? AND c.escalation_level = ? AND c.assigned_designation = ?";
        }
        sql += " ORDER BY c.priority_score DESC, c.submitted_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!isPrincipal) {
                ps.setString(1, authority.getAuthorityCategory());
                ps.setInt(2, authority.getAuthorityLevel());
                ps.setString(3, authority.getDesignation());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapViewRow(rs));
            }
        }
        return list;
    }

    public boolean authorityOwnsComplaint(User authority, int complaintId) throws SQLException {
        Complaint c = getComplaintById(complaintId);
        if (c == null || "RESOLVED".equals(c.getStatus())) return false;

        if (authority.getAuthorityLevel() != null && authority.getAuthorityLevel() == 2) {
            return c.getEscalationLevel() == 2 && "Principal".equals(c.getAssignedDesignation());
        }
        return authority.getAuthorityCategory() != null
                && authority.getAuthorityCategory().equalsIgnoreCase(c.getCategory())
                && authority.getAuthorityLevel() != null
                && authority.getAuthorityLevel() == c.getEscalationLevel()
                && authority.getDesignation() != null
                && authority.getDesignation().equals(c.getAssignedDesignation());
    }

    public List<Complaint> getAllComplaints() throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints ORDER BY priority_score DESC, submitted_date ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Complaint> getEscalatedComplaints() throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE status = 'ESCALATED' ORDER BY escalation_level DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Complaint getComplaintById(int id) throws SQLException {
        String sql = "SELECT * FROM complaints WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public void resolveComplaint(int id) throws SQLException {
        String sql = "UPDATE complaints SET status = 'RESOLVED' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Complaint> getOverdueOpenComplaints() throws SQLException {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT * FROM complaints WHERE status IN ('OPEN', 'ESCALATED') " +
                "AND escalation_level < 2 AND deadline < NOW()";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void escalate(int complaintId, int newLevel, String escalatedTo, String reason) throws SQLException {
        String updateSql = "UPDATE complaints SET escalation_level = ?, status = 'ESCALATED', " +
                "assigned_designation = ?, deadline = (NOW() + INTERVAL 48 HOUR) WHERE id = ?";
        String logSql = "INSERT INTO escalation_log (complaint_id, escalated_to, escalated_date, reason) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(updateSql)) {
                ps1.setInt(1, newLevel);
                ps1.setString(2, escalatedTo);
                ps1.setInt(3, complaintId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(logSql)) {
                ps2.setInt(1, complaintId);
                ps2.setString(2, escalatedTo);
                ps2.setString(3, reason);
                ps2.executeUpdate();
            }
            conn.commit();
        }
    }

    /** Backfill routing columns for rows created before authority mapping existed. */
    public void backfillAssignmentColumns() throws SQLException {
        String sql = "SELECT id, category, escalation_level FROM complaints WHERE assigned_designation IS NULL OR level0_designation IS NULL";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String cat = rs.getString("category");
                int level = rs.getInt("escalation_level");
                String l0 = AuthorityMapper.getDesignation(cat, 0);
                String assigned = AuthorityMapper.getDesignation(cat, level);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE complaints SET level0_designation = ?, assigned_designation = ? WHERE id = ?")) {
                    ps.setString(1, l0);
                    ps.setString(2, assigned);
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
            }
        }
    }

    private Complaint mapRow(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getInt("id"));
        c.setStudentName(rs.getString("student_name"));
        c.setCategory(rs.getString("category"));
        c.setDescription(rs.getString("description"));
        c.setSubmittedDate(rs.getTimestamp("submitted_date").toLocalDateTime());
        c.setDeadline(rs.getTimestamp("deadline").toLocalDateTime());
        c.setStatus(rs.getString("status"));
        c.setPriorityScore(rs.getInt("priority_score"));
        c.setPriorityTier(rs.getString("priority_tier"));
        c.setEscalationLevel(rs.getInt("escalation_level"));
        c.setAssignedDesignation(rs.getString("assigned_designation"));
        c.setLevel0Designation(rs.getString("level0_designation"));
        return c;
    }

    private ComplaintView mapViewRow(ResultSet rs) throws SQLException {
        ComplaintView v = new ComplaintView();
        Complaint base = mapRow(rs);
        v.setId(base.getId());
        v.setStudentName(base.getStudentName());
        v.setCategory(base.getCategory());
        v.setDescription(base.getDescription());
        v.setSubmittedDate(base.getSubmittedDate());
        v.setDeadline(base.getDeadline());
        v.setStatus(base.getStatus());
        v.setPriorityScore(base.getPriorityScore());
        v.setPriorityTier(base.getPriorityTier());
        v.setEscalationLevel(base.getEscalationLevel());
        v.setAssignedDesignation(base.getAssignedDesignation());
        v.setLevel0Designation(base.getLevel0Designation());
        v.setLevel0Authority(base.getLevel0Designation() != null
                ? base.getLevel0Designation()
                : AuthorityMapper.getDesignation(base.getCategory(), 0));
        v.setCategoryLabel(AuthorityMapper.categoryLabel(base.getCategory()));
        v.setStudentEmail(rs.getString("student_email"));
        v.setStudentPhone(rs.getString("student_phone"));
        return v;
    }
}
