public class Main {
    public static void main(String[] args) {
        java.util.logging.Logger.getLogger("org.mongodb.driver").setLevel(java.util.logging.Level.WARNING);
        int port = 8080;

        // Можно передать порт через аргументы командной строки
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("⚠️  Invalid port number, using default 8080");
            }
        }

        try {
            WebServer server = new WebServer(port);
            server.start();

            // Добавляем shutdown hook для корректного завершения
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n⏹️  Shutting down server...");
                server.stop();
            }));

            // Ждем нажатия Enter для остановки сервера
            System.in.read();

            server.stop();

        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}