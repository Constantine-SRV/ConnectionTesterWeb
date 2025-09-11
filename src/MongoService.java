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
                if (commandJson != null && !commandJson.trim().isEmpty()) {
                    try {
                        // Парсим JSON команду
                        commandToExecute = Document.parse(commandJson);
                        result.data.put("executedCommand", commandJson);
                    } catch (Exception e) {
                        // Если не удалось распарсить, используем hello
                        commandToExecute = new Document("hello", 1);
                        result.data.put("commandParseError", "Failed to parse command: " + e.getMessage());
                        result.data.put("executedCommand", "hello (fallback)");
                    }
                } else {
                    commandToExecute = new Document("hello", 1);
                    result.data.put("executedCommand", "hello (default)");
                }

                // Выполняем команду
                try {
                    Document commandResult = adminDatabase.runCommand(commandToExecute);

                    // Добавляем результат команды
                    Map<String, Object> cmdResult = new HashMap<>();
                    for (String key : commandResult.keySet()) {
                        Object value = commandResult.get(key);
                        // Ограничиваем вывод больших объектов
                        if (value instanceof List && ((List<?>)value).size() > 10) {
                            List<?> list = (List<?>) value;
                            cmdResult.put(key, list.subList(0, 10) + " ... (showing first 10 of " + list.size() + ")");
                        } else {
                            cmdResult.put(key, value);
                        }
                    }
                    result.data.put("commandResult", cmdResult);

                    // Если это hello команда, извлекаем основную информацию
                    if (commandToExecute.containsKey("hello")) {
                        result.data.put("isWritablePrimary", commandResult.getBoolean("isWritablePrimary"));
                        result.data.put("hosts", commandResult.get("hosts"));
                        result.data.put("primary", commandResult.get("primary"));
                        result.data.put("me", commandResult.get("me"));
                    }

                } catch (Exception e) {
                    result.data.put("commandError", e.getMessage());

                    // Пробуем выполнить базовые команды для диагностики
                    try {
                        // Пробуем ping
                        Document pingResult = adminDatabase.runCommand(new Document("ping", 1));
                        result.data.put("ping", pingResult.get("ok"));
                    } catch (Exception pingEx) {
                        result.data.put("pingError", pingEx.getMessage());
                    }
                }

                // Пробуем получить список баз данных если есть права
                try {
                    List<Map<String, Object>> databases = new ArrayList<>();
                    for (Document db : mongoClient.listDatabases()) {
                        Map<String, Object> dbInfo = new HashMap<>();
                        dbInfo.put("name", db.getString("name"));
                        Number size = (Number) db.get("sizeOnDisk");
                        dbInfo.put("sizeOnDisk", size);
                        databases.add(dbInfo);
                    }
                    result.data.put("databases", databases);
                    result.data.put("databaseCount", databases.size());
                } catch (Exception e) {
                    result.data.put("databasesError", "No permission to list databases");
                }

                // Информация о сервере если доступна
                try {
                    Document serverStatus = adminDatabase.runCommand(new Document("serverStatus", 1));
                    result.data.put("version", serverStatus.get("version"));
                    result.data.put("uptime", serverStatus.get("uptime"));
                    result.data.put("host", serverStatus.get("host"));
                } catch (Exception e) {
                    result.data.put("serverStatusError", "No permission for serverStatus");
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