package handler;

import com.sun.net.httpserver.HttpExchange;
import db.UserDAO;
import model.User;
import service.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

public class AuthHelper {

    private static final UserDAO userDAO = new UserDAO();

    public static Optional<User> getCurrentUser(HttpExchange exchange) throws SQLException {
        String token = extractToken(exchange);
        Optional<Integer> userId = SessionManager.getInstance().getUserId(token);
        if (userId.isEmpty()) return Optional.empty();
        return userDAO.getById(userId.get());
    }

    public static User requireUser(HttpExchange exchange) throws SQLException, AuthException {
        return getCurrentUser(exchange).orElseThrow(() -> new AuthException(401, "Login required"));
    }

    public static User requireAdmin(HttpExchange exchange) throws SQLException, AuthException {
        User user = requireUser(exchange);
        if (!user.isAdmin()) {
            throw new AuthException(403, "Admin access required");
        }
        return user;
    }

    public static User requireAuthority(HttpExchange exchange) throws SQLException, AuthException {
        User user = requireUser(exchange);
        if (!user.isAuthority()) {
            throw new AuthException(403, "Authority access required");
        }
        return user;
    }

    public static User requireStudent(HttpExchange exchange) throws SQLException, AuthException {
        User user = requireUser(exchange);
        if (!user.isStudent()) {
            throw new AuthException(403, "Student access required");
        }
        return user;
    }

    public static String extractToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return exchange.getRequestHeaders().getFirst("X-Session-Token");
    }

    public static class AuthException extends Exception {
        private final int statusCode;

        public AuthException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() { return statusCode; }
    }
}
