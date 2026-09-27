package service;

import db.UserDAO;
import util.PasswordUtil;

import java.sql.SQLException;

/**
 * Seeds one login per authority position (not self-registered).
 * Passwords are printed to the console on first creation only.
 */
public class AuthoritySeeder {

    private final UserDAO userDAO = new UserDAO();

    private record Slot(String username, String fullName, String designation, String category, int level) {}

    private static final Slot[] SLOTS = {
            new Slot("auth_mess_l0", "Mess Committee Lead", "Mess Committee", "mess", 0),
            new Slot("auth_mess_l1", "Hostel Warden (Mess)", "Hostel Warden", "mess", 1),
            new Slot("auth_hostel_l0", "Hostel Warden", "Hostel Warden", "hostel", 0),
            new Slot("auth_hostel_l1", "Dean of Student Affairs (Hostel)", "Dean of Student Affairs", "hostel", 1),
            new Slot("auth_faculty_l0", "Department HOD", "Department HOD", "faculty", 0),
            new Slot("auth_faculty_l1", "Dean of Academics", "Dean of Academics", "faculty", 1),
            new Slot("auth_electrical_l0", "Maintenance / Estate Office", "Maintenance/Estate Office", "electrical", 0),
            new Slot("auth_electrical_l1", "Estate Officer (Electrical)", "Estate Officer", "electrical", 1),
            new Slot("auth_water_l0", "Estate Office (Water)", "Estate Office", "water", 0),
            new Slot("auth_water_l1", "Estate Officer (Water)", "Estate Officer", "water", 1),
            new Slot("auth_harassment_l0", "Anti-Ragging Committee", "Anti-Ragging Committee", "harassment", 0),
            new Slot("auth_harassment_l1", "Dean of Student Affairs (Harassment)", "Dean of Student Affairs", "harassment", 1),
            new Slot("auth_other_l0", "Department Office", "Department Office", "other", 0),
            new Slot("auth_other_l1", "Warden / HOD (Other)", "Warden/HOD", "other", 1),
            new Slot("auth_principal_l2", "Principal", "Principal", "all", 2),
    };

    public void seed() throws SQLException {
        userDAO.ensureSchema();
        boolean anyNew = false;

        System.out.println();
        System.out.println("========================================");
        System.out.println("  AUTHORITY ACCOUNTS (first-time only)");
        System.out.println("========================================");

        for (Slot slot : SLOTS) {
            if (userDAO.authorityAccountExists(slot.username())) {
                continue;
            }
            String password = PasswordUtil.randomPassword(10);
            userDAO.createAuthorityUser(
                    slot.username(), password, slot.fullName(),
                    slot.designation(), slot.category(), slot.level()
            );
            anyNew = true;
            System.out.printf("  %-22s  %-38s  %s%n", slot.username(), slot.designation() + " (L" + slot.level() + ")", password);
        }

        if (!anyNew) {
            System.out.println("  (All authority accounts already exist — passwords not re-printed)");
        }
        System.out.println("========================================");
        System.out.println();
    }
}
