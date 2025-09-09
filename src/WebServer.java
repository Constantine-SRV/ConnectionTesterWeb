import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {
    private HttpServer server;
    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Регистрируем обработчики
        server.createContext("/", new ApiHandler.RootHandler());
        server.createContext("/api/test/mongo", new ApiHandler.MongoTestHandler());
        server.createContext("/api/test/postgres", new ApiHandler.PostgresTestHandler());

        server.setExecutor(null); // создаст дефолтный executor
        server.start();

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║       DATABASE CONNECTION TESTER SERVER         ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ 🚀 Server started on http://localhost:" + port + "       ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ 📌 Available endpoints:                          ║");
        System.out.println("║    • Web UI: http://localhost:" + port + "/             ║");
        System.out.println("║    • MongoDB API: /api/test/mongo                ║");
        System.out.println("║    • PostgreSQL API: /api/test/postgres          ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ 📝 API Usage:                                    ║");
        System.out.println("║    GET /api/test/mongo?connection=<url>          ║");
        System.out.println("║    GET /api/test/postgres?connection=<url>       ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ 🛑 Press Enter to stop the server                ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("\n✅ Server stopped successfully");
        }
    }
}