import com.sun.net.httpserver.HttpServer;
import db.DBConnection;
import db.ComplaintDAO;
import handler.AuthHandler;
import handler.AuthorityHandler;
import handler.ComplaintHandler;
import handler.StaticFileHandler;
import service.AdminSeeder;
import service.AuthoritySeeder;
import service.EscalationService;
import util.AppConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        if (!DBConnection.testConnection()) {
            System.err.println();
            System.err.println("Could not connect to MySQL.");
            System.err.println("1. Make sure MySQL is running.");
            System.err.println("2. Run schema.sql to create the complaint_tracker database.");
            System.err.println("3. Edit config.properties with your db.user and db.password.");
            System.exit(1);
        }

        try {
            new AdminSeeder().seed();
            new AuthoritySeeder().seed();
            new ComplaintDAO().backfillAssignmentColumns();
        } catch (Exception e) {
            System.err.println("Failed to initialize auth schema: " + e.getMessage());
            System.exit(1);
        }

        int port = AppConfig.getInt("server.port", 8081);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/complaints", new ComplaintHandler());
        server.createContext("/api/authority", new AuthorityHandler());

        Path publicDir = Paths.get(System.getProperty("user.dir"), "public");
        server.createContext("/", new StaticFileHandler(publicDir));

        server.setExecutor(null);
        server.start();

        new EscalationService().start(1);

        System.out.println("Server started at http://localhost:" + port);
        System.out.println("Escalation engine running in background...");
    }
}
