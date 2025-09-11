import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class MongoService {

    public static class TestResult {
        public boolean success;
        public String message;
        public Map<String, Object> data;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.data = new HashMap<>();
        }
    }

    public static TestResult testConnection(String connectionString) {
        return testConnection(connectionString, null);
    }

    public static TestResult testConnection(String connectionString, String commandJson) {
        try {
            // Валидация строки подключения
            if (connectionString == null || connectionString.isEmpty()) {
                return new TestResult(false, "Connection string is empty");
            }

            if (!connectionString.startsWith("mongodb://")) {
                return new TestResult(false, "Connection string must start with mongodb://");
            }

            MongoClientURI uri = new MongoClientURI(connectionString);
            TestResult result = new TestResult(true, "MongoDB connection successful");

            try (MongoClient mongoClient = new MongoClient(uri)) {
                // Получаем базу данных admin
                MongoDatabase adminDatabase = mongoClient.getDatabase("admin");

                // Выполняем команду - либо переданную, либо hello по умолчанию
                Document commandToExecute;
                String commandName = "hello";

                if (commandJson != null && !commandJson.trim().isEmpty()) {
                    try {
                        // Парсим JSON команду
                        commandToExecute = Document.parse(commandJson);
                        result.data.put("executedCommand", commandJson);

                        // Определяем имя команды для логирования
                        if (!commandToExecute.isEmpty()) {
                            commandName = commandToExecute.keySet().iterator().next();
                        }
                    } catch (Exception e) {
                        // Если не удалось распарсить, возвращаем ошибку
                        result.success = false;
                        result.message = "Invalid JSON command format";
                        result.data.put("error", e.getMessage());
                        result.data.put("hint", "Use valid JSON like: {\"ping\": 1} or {\"listCollections\": 1}");
                        result.data.put("examples", new String[]{
                                "{\"ping\": 1}",
                                "{\"hello\": 1}",
                                "{\"listDatabases\": 1}",
                                "{\"listCollections\": 1}",
                                "{\"serverStatus\": 1}",
                                "{\"connectionStatus\": 1}",
                                "{\"currentOp\": 1}",
                                "{\"replSetGetStatus\": 1}"
                        });
                        return result;
                    }
                } else {
                    commandToExecute = new Document("hello", 1);
                    result.data.put("executedCommand", "hello (default)");
                }

                // Выполняем только запрошенную команду
                try {
                    Document commandResult = adminDatabase.runCommand(commandToExecute);

                    // Добавляем результат команды
                    Map<String, Object> cmdResult = new HashMap<>();
                    for (String key : commandResult.keySet()) {
                        Object value = commandResult.get(key);
                        // Ограничиваем вывод больших объектов
                        if (value instanceof List && ((List<?>)value).size() > 10) {
                            List<?> list = (List<?>) value;
                            List<Object> preview = new ArrayList<>();
                            for (int i = 0; i < Math.min(10, list.size()); i++) {
                                preview.add(list.get(i));
                            }
                            cmdResult.put(key, preview);
                            cmdResult.put(key + "_total_count", list.size());
                            cmdResult.put(key + "_note", "Showing first 10 of " + list.size());
                        } else {
                            cmdResult.put(key, value);
                        }
                    }
                    result.data.put("commandResult", cmdResult);

                    // Добавляем дополнительную информацию в зависимости от команды
                    if (commandName.equals("hello") && commandResult.getBoolean("isWritablePrimary") != null) {
                        result.data.put("connectionInfo", Map.of(
                                "isWritablePrimary", commandResult.getBoolean("isWritablePrimary"),
                                "hosts", commandResult.get("hosts"),
                                "primary", commandResult.get("primary")
                        ));
                    }

                } catch (Exception e) {
                    result.success = false;
                    result.message = "Command execution failed";
                    result.data.put("commandError", e.getMessage());
                    result.data.put("executedCommand", commandName);

                    // Даем подсказку по возможной причине
                    if (e.getMessage().contains("not authorized") || e.getMessage().contains("permission")) {
                        result.data.put("hint", "User may not have permission to execute this command");
                    } else if (e.getMessage().contains("UnknownHostException") || e.getMessage().contains("Timed out")) {
                        result.data.put("hint", "Cannot connect to MongoDB server. Check host and port");
                    }
                }

                return result;

            } catch (Exception e) {
                return new TestResult(false, "MongoDB connection failed: " + e.getMessage());
            }

        } catch (Exception e) {
            return new TestResult(false, "Invalid MongoDB connection string: " + e.getMessage());
        }
    }
}