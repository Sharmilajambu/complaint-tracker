package service;

import db.ComplaintDAO;
import model.Complaint;

import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class EscalationService {

    private final ComplaintDAO dao = new ComplaintDAO();
    private final Timer timer = new Timer(true);

    public void start(int intervalMinutes) {
        long intervalMs = intervalMinutes * 60L * 1000L;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAndEscalate();
            }
        }, 0, intervalMs);
    }

    private void checkAndEscalate() {
        try {
            List<Complaint> overdue = dao.getOverdueOpenComplaints();
            for (Complaint c : overdue) {
                int nextLevel = c.getEscalationLevel() + 1;
                if (nextLevel > 2) continue;
                String escalatedTo = AuthorityMapper.getDesignation(c.getCategory(), nextLevel);
                String reason = "Deadline passed without resolution (priority: " + c.getPriorityTier() + ")";
                dao.escalate(c.getId(), nextLevel, escalatedTo, reason);
                System.out.println("[Escalation] Complaint #" + c.getId() + " → " + escalatedTo + " (L" + nextLevel + ")");
            }
        } catch (SQLException e) {
            System.err.println("Escalation check failed: " + e.getMessage());
        }
    }
}
