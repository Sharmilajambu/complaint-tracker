package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import db.ComplaintDAO;
import model.Complaint;
import model.User;
import util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * All routes require a valid session token.
 *
 *   GET  /api/complaints             -> admin: all | student: own only
 *   GET  /api/complaints/escalated   -> admin only
 *   POST /api/complaints             -> create (student_name from session)
 *   POST /api/complaints/{id}/resolve -> admin only
 */
public class ComplaintHandler implements HttpHandler {

    private final ComplaintDAO dao = new ComplaintDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equals("OPTIONS")) {
                sendResponse(exchange, 204, "");
                return;
            }

            User user = AuthHelper.requireUser(exchange);

            if (method.equals("GET") && path.equals("/api/complaints")) {
                handleGetAll(exchange, user);
            } else if (method.equals("GET") && path.equals("/api/complaints/escalated")) {
                handleGetEscalated(exchange, user);
            } else if (method.equals("POST") && path.equals("/api/complaints")) {
                handleCreate(exchange, user);
            } else if (method.equals("POST") && path.matches("/api/complaints/\\d+/resolve")) {
                handleResolve(exchange, user, path);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (AuthHelper.AuthException e) {
            sendResponse(exchange, e.getStatusCode(), "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            sendResponse(exchange, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange, User user) throws IOException, SQLException, AuthHelper.AuthException {
        if (user.isAuthority()) {
            throw new AuthHelper.AuthException(403, "Use the authority dashboard");
        }
        List<Complaint> list = user.isAdmin()
                ? dao.getAllComplaints()
                : dao.getComplaintsByUserId(user.getId());
        sendResponse(exchange, 200, JsonUtil.toJsonArray(list));
    }

    private void handleGetEscalated(HttpExchange exchange, User user) throws IOException, SQLException, AuthHelper.AuthException {
        AuthHelper.requireAdmin(exchange);
        List<Complaint> escalated = dao.getEscalatedComplaints();
        sendResponse(exchange, 200, JsonUtil.toJsonArray(escalated));
    }

    private void handleCreate(HttpExchange exchange, User user) throws IOException, SQLException, AuthHelper.AuthException {
        if (!user.isStudent()) {
            throw new AuthHelper.AuthException(403, "Only students can submit complaints");
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = JsonUtil.parseFlatObject(body);

        String category = fields.getOrDefault("category", "other");
        String description = fields.getOrDefault("description", "");

        if (description.isBlank()) {
            sendResponse(exchange, 400, "{\"error\":\"description is required\"}");
            return;
        }

        Complaint created = dao.createComplaint(user.getId(), user.getFullName(), category, description);
        sendResponse(exchange, 201, JsonUtil.toJson(created));
    }

    private void handleResolve(HttpExchange exchange, User user, String path) throws IOException, SQLException, AuthHelper.AuthException {
        AuthHelper.requireAdmin(exchange);
        String idStr = path.split("/")[3];
        int id = Integer.parseInt(idStr);
        dao.resolveComplaint(id);
        sendResponse(exchange, 200, "{\"status\":\"resolved\"}");
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Session-Token");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);
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
