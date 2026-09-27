package service;

import db.UserDAO;
import util.PasswordUtil;

import java.sql.SQLException;

/**
 * Ensures one ADMIN account exists. Admin accounts are never self-registered —
 * they are bootstrapped here on first startup with a random password printed to the console.
 */
public class AdminSeeder {

    private final UserDAO userDAO = new UserDAO();

    public void seed() throws SQLException {
        userDAO.ensureSchema();

        if (userDAO.adminExists()) {
            System.out.println("[Auth] Admin account already exists (username: admin)");
            return;
        }

        String password = PasswordUtil.randomPassword(12);
        userDAO.createUser("admin", password, "System Administrator", "ADMIN");

        System.out.println();
        System.out.println("========================================");
        System.out.println("  ADMIN ACCOUNT CREATED (first run)");
        System.out.println("  Username : admin");
        System.out.println("  Password : " + password);
        System.out.println("  (Save this — not shown again)");
        System.out.println("========================================");
        System.out.println();
    }
}
