package util;

import model.Complaint;
import model.ComplaintView;
import model.User;
import service.AuthorityMapper;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very small hand-rolled JSON helper - just enough for this project.
 * Avoids needing an external library (Gson/Jackson) so the whole app
 * stays "core Java" with zero extra dependencies besides the MySQL driver.
 */
public class JsonUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    public static String toJson(Complaint c) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(c.getId()).append(",");
        sb.append("\"studentName\":\"").append(esc(c.getStudentName())).append("\",");
        sb.append("\"category\":\"").append(esc(c.getCategory())).append("\",");
        sb.append("\"description\":\"").append(esc(c.getDescription())).append("\",");
        sb.append("\"submittedDate\":\"").append(c.getSubmittedDate().format(FMT)).append("\",");
        sb.append("\"deadline\":\"").append(c.getDeadline().format(FMT)).append("\",");
        sb.append("\"status\":\"").append(esc(c.getStatus())).append("\",");
        sb.append("\"priorityScore\":").append(c.getPriorityScore()).append(",");
        sb.append("\"priorityTier\":\"").append(esc(c.getPriorityTier())).append("\",");
        sb.append("\"escalationLevel\":").append(c.getEscalationLevel());
        sb.append("}");
        return sb.toString();
    }

    public static String toUserJson(User u, String token) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(u.getId()).append(",");
        sb.append("\"username\":\"").append(esc(u.getUsername())).append("\",");
        sb.append("\"fullName\":\"").append(esc(u.getFullName())).append("\",");
        sb.append("\"role\":\"").append(esc(u.getRole())).append("\",");
        sb.append("\"token\":\"").append(esc(token)).append("\"");
        if (u.isAuthority()) {
            sb.append(",\"designation\":\"").append(esc(u.getDesignation())).append("\"");
            sb.append(",\"authorityCategory\":\"").append(esc(u.getAuthorityCategory())).append("\"");
            if (u.getAuthorityLevel() != null) {
                sb.append(",\"authorityLevel\":").append(u.getAuthorityLevel());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toAuthorityProfileJson(User u) {
        String cat = u.getAuthorityCategory();
        String categoryLabel = "all".equalsIgnoreCase(cat)
                ? "All categories (final escalation)"
                : AuthorityMapper.categoryLabel(cat);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"fullName\":\"").append(esc(u.getFullName())).append("\",");
        sb.append("\"designation\":\"").append(esc(u.getDesignation())).append("\",");
        sb.append("\"authorityCategory\":\"").append(esc(cat)).append("\",");
        sb.append("\"categoryLabel\":\"").append(esc(categoryLabel)).append("\"");
        if (u.getAuthorityLevel() != null) {
            sb.append(",\"authorityLevel\":").append(u.getAuthorityLevel());
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toAuthorityComplaintJson(ComplaintView v) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(v.getId()).append(",");
        sb.append("\"studentName\":\"").append(esc(v.getStudentName())).append("\",");
        sb.append("\"category\":\"").append(esc(v.getCategory())).append("\",");
        sb.append("\"categoryLabel\":\"").append(esc(v.getCategoryLabel())).append("\",");
        sb.append("\"description\":\"").append(esc(v.getDescription())).append("\",");
        sb.append("\"submittedDate\":\"").append(v.getSubmittedDate().format(FMT)).append("\",");
        sb.append("\"deadline\":\"").append(v.getDeadline().format(FMT)).append("\",");
        sb.append("\"status\":\"").append(esc(v.getStatus())).append("\",");
        sb.append("\"priorityScore\":").append(v.getPriorityScore()).append(",");
        sb.append("\"priorityTier\":\"").append(esc(v.getPriorityTier())).append("\",");
        sb.append("\"escalationLevel\":").append(v.getEscalationLevel()).append(",");
        sb.append("\"assignedDesignation\":\"").append(esc(v.getAssignedDesignation())).append("\",");
        sb.append("\"level0Authority\":\"").append(esc(v.getLevel0Authority())).append("\",");
        sb.append("\"studentEmail\":\"").append(esc(v.getStudentEmail())).append("\",");
        sb.append("\"studentPhone\":\"").append(esc(v.getStudentPhone())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public static String toAuthorityComplaintArray(List<ComplaintView> complaints) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < complaints.size(); i++) {
            sb.append(toAuthorityComplaintJson(complaints.get(i)));
            if (i < complaints.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJsonArray(List<Complaint> complaints) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < complaints.size(); i++) {
            sb.append(toJson(complaints.get(i)));
            if (i < complaints.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extremely simple JSON object parser - only handles flat {"key":"value"} objects,
     * which is all the frontend ever sends (new complaint submissions).
     */
    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        Pattern p = Pattern.compile("\"(.*?)\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(json);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }
}
