package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import db.UserDAO;
import model.User;
import service.SessionManager;
import util.JsonUtil;
import util.PasswordUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 *   POST /api/auth/register  -> student self-registration (role always STUDENT)
 *   POST /api/auth/login     -> returns session token
 *   POST /api/auth/logout    -> invalidates token
 *   GET  /api/auth/me        -> current user info
 */
public class AuthHandler implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equals("OPTIONS")) {
                send(exchange, 204, "");
                return;
            }

            if (method.equals("POST") && path.equals("/api/auth/register")) {
                handleRegister(exchange);
            } else if (method.equals("POST") && path.equals("/api/auth/login")) {
                handleLogin(exchange);
            } else if (method.equals("POST") && path.equals("/api/auth/logout")) {
                handleLogout(exchange);
            } else if (method.equals("GET") && path.equals("/api/auth/me")) {
                handleMe(exchange);
            } else {
                send(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (AuthHelper.AuthException e) {
            send(exchange, e.getStatusCode(), "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            send(exchange, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> fields = parseBody(exchange);

        String fullName = fields.getOrDefault("fullName", "").trim();
        String username = fields.getOrDefault("username", "").trim().toLowerCase();
        String password = fields.getOrDefault("password", "");
        String email = fields.getOrDefault("email", "").trim();
        String phone = fields.getOrDefault("phone", "").trim();

        if (fullName.isBlank()) {
            send(exchange, 400, "{\"error\":\"Full name is required\"}");
            return;
        }
        if (username.length() < 3) {
            send(exchange, 400, "{\"error\":\"Username must be at least 3 characters\"}");
            return;
        }
        if (password.length() < 6) {
            send(exchange, 400, "{\"error\":\"Password must be at least 6 characters\"}");
            return;
        }
        if ("admin".equals(username) || username.startsWith("auth_")) {
            send(exchange, 400, "{\"error\":\"This username is reserved\"}");
            return;
        }

        if (userDAO.findByUsername(username).isPresent()) {
            send(exchange, 409, "{\"error\":\"Username already taken\"}");
            return;
        }

        User created = userDAO.createStudent(username, password, fullName, email.isBlank() ? null : email, phone.isBlank() ? null : phone);
        String token = SessionManager.getInstance().createSession(created);
        send(exchange, 201, JsonUtil.toUserJson(created, token));
    }

    private void handleLogin(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> fields = parseBody(exchange);

        String username = fields.getOrDefault("username", "").trim().toLowerCase();
        String password = fields.getOrDefault("password", "");

        User user = userDAO.findByUsername(username).orElse(null);
        if (user == null || !PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
            send(exchange, 401, "{\"error\":\"Invalid username or password\"}");
            return;
        }

        String token = SessionManager.getInstance().createSession(user);
        send(exchange, 200, JsonUtil.toUserJson(user, token));
    }

    private void handleLogout(HttpExchange exchange) throws IOException, AuthHelper.AuthException, SQLException {
        AuthHelper.requireUser(exchange);
        String token = AuthHelper.extractToken(exchange);
        SessionManager.getInstance().invalidate(token);
        send(exchange, 200, "{\"status\":\"logged out\"}");
    }

    private void handleMe(HttpExchange exchange) throws IOException, AuthHelper.AuthException, SQLException {
        User user = AuthHelper.requireUser(exchange);
        send(exchange, 200, JsonUtil.toUserJson(user, AuthHelper.extractToken(exchange)));
    }

    private Map<String, String> parseBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.parseFlatObject(body);
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-Token");
    }

    private void send(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.close();
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "'");
    }
}
