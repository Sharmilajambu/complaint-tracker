package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import db.ComplaintDAO;
import model.ComplaintView;
import model.User;
import util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/**
 * Authority dashboard API (separate from admin/student complaint routes).
 *
 *   GET  /api/authority/profile
 *   GET  /api/authority/complaints
 *   POST /api/authority/complaints/{id}/resolve
 */
public class AuthorityHandler implements HttpHandler {

    private final ComplaintDAO dao = new ComplaintDAO();

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

            User user = AuthHelper.requireAuthority(exchange);

            if (method.equals("GET") && path.equals("/api/authority/profile")) {
                send(exchange, 200, JsonUtil.toAuthorityProfileJson(user));
            } else if (method.equals("GET") && path.equals("/api/authority/complaints")) {
                handleList(exchange, user);
            } else if (method.equals("POST") && path.matches("/api/authority/complaints/\\d+/resolve")) {
                handleResolve(exchange, user, path);
            } else {
                send(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (AuthHelper.AuthException e) {
            send(exchange, e.getStatusCode(), "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            send(exchange, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleList(HttpExchange exchange, User user) throws IOException, SQLException {
        List<ComplaintView> list = dao.getComplaintsForAuthority(user);
        send(exchange, 200, JsonUtil.toAuthorityComplaintArray(list));
    }

    private void handleResolve(HttpExchange exchange, User user, String path) throws IOException, SQLException, AuthHelper.AuthException {
        int id = Integer.parseInt(path.split("/")[4]);
        if (!dao.authorityOwnsComplaint(user, id)) {
            throw new AuthHelper.AuthException(403, "This complaint is not assigned to you");
        }
        dao.resolveComplaint(id);
        send(exchange, 200, "{\"status\":\"resolved\"}");
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
