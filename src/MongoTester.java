import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class MongoTester {

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
        try {
            // Валидация строки подключения
            if (connectionString == null || connectionString.isEmpty()) {
                return new TestResult(false, "Connection string is empty");
            }

            if (!connectionString.startsWith("mongodb://")) {
                return new TestResult(false, "Connection string must start with mongodb://");
            }

            MongoClientURI uri = new MongoClientURI(connectionString);
            TestResult result = new TestResult(true, "Connection successful");

            try (MongoClient mongoClient = new MongoClient(uri)) {
                // Получаем базу данных admin
                MongoDatabase adminDatabase = mongoClient.getDatabase("admin");

                // Выполняем команду hello
                Document helloResult = adminDatabase.runCommand(new Document("hello", 1));
                result.data.put("isWritablePrimary", helloResult.getBoolean("isWritablePrimary"));
                result.data.put("hosts", helloResult.get("hosts"));
                result.data.put("primary", helloResult.get("primary"));
                result.data.put("me", helloResult.get("me"));

                // Получаем список баз данных
                List<String> databases = new ArrayList<>();
                for (Document db : mongoClient.listDatabases()) {
                    databases.add(db.getString("name"));
                }
                result.data.put("databases", databases);
                result.data.put("databaseCount", databases.size());

                // Информация о сервере
                Document serverStatus = adminDatabase.runCommand(new Document("serverStatus", 1));
                result.data.put("version", serverStatus.get("version"));
                result.data.put("uptime", serverStatus.get("uptime"));
                result.data.put("host", serverStatus.get("host"));

                // Статус replica set (если есть)
                try {
                    Document replStatus = adminDatabase.runCommand(new Document("replSetGetStatus", 1));
                    Map<String, Object> replicaInfo = new HashMap<>();
                    replicaInfo.put("set", replStatus.getString("set"));
                    replicaInfo.put("myState", replStatus.getInteger("myState"));
                    result.data.put("replicaSet", replicaInfo);
                } catch (Exception e) {
                    // Не replica set - это нормально
                    result.data.put("replicaSet", "Not a replica set");
                }

                return result;

            } catch (Exception e) {
                return new TestResult(false, "Connection failed: " + e.getMessage());
            }

        } catch (Exception e) {
            return new TestResult(false, "Invalid connection string: " + e.getMessage());
        }
    }

    // Полный тест с записью (можно вызывать отдельно)
    public static TestResult fullTest(String connectionString) {
        TestResult result = testConnection(connectionString);
        if (!result.success) {
            return result;
        }

        try {
            MongoClientURI uri = new MongoClientURI(connectionString);
            try (MongoClient mongoClient = new MongoClient(uri)) {
                // Тест записи/чтения
                var testDb = mongoClient.getDatabase("test");
                var collection = testDb.getCollection("test_java_web");

                // Вставляем документ
                Document testDoc = new Document("message", "Test from Web API")
                        .append("timestamp", new java.util.Date())
                        .append("random", Math.random());

                collection.insertOne(testDoc);
                result.data.put("writeTest", "Document inserted successfully");

                // Читаем документ
                Document found = collection.find().sort(new Document("_id", -1)).first();
                if (found != null) {
                    result.data.put("readTest", "Document read successfully");
                    result.data.put("lastDocument", found.toJson());
                }

                long count = collection.countDocuments();
                result.data.put("documentCount", count);

            }
        } catch (Exception e) {
            result.message += " (Write test failed: " + e.getMessage() + ")";
        }

        return result;
    }
}