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

                // Выполняем команду hello
                Document helloResult = adminDatabase.runCommand(new Document("hello", 1));
                result.data.put("isWritablePrimary", helloResult.getBoolean("isWritablePrimary"));
                result.data.put("hosts", helloResult.get("hosts"));
                result.data.put("primary", helloResult.get("primary"));
                result.data.put("me", helloResult.get("me"));

                // Получаем список баз данных
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

                    // Информация о членах репликасета
                    List<Map<String, Object>> members = new ArrayList<>();
                    if (replStatus.containsKey("members")) {
                        for (Document member : replStatus.getList("members", Document.class)) {
                            Map<String, Object> memberInfo = new HashMap<>();
                            memberInfo.put("name", member.getString("name"));
                            memberInfo.put("state", member.getString("stateStr"));
                            members.add(memberInfo);
                        }
                    }
                    replicaInfo.put("members", members);
                    result.data.put("replicaSet", replicaInfo);
                } catch (Exception e) {
                    // Не replica set - это нормально
                    result.data.put("replicaSet", "Not a replica set or access denied");
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