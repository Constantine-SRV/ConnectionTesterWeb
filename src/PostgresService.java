import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class PostgresService {

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

            // PostgreSQL connection strings могут быть разные
            // jdbc:postgresql://host:port/database
            // postgresql://user:psw@host:port/database

            String jdbcUrl = connectionString;

            // Преобразуем если это не JDBC формат
            if (connectionString.startsWith("postgresql://")) {
                jdbcUrl = convertToJdbcUrl(connectionString);
            } else if (!connectionString.startsWith("jdbc:postgresql://")) {
                return new TestResult(false, "Connection string must start with postgresql:// or jdbc:postgresql://");
            }

            TestResult result = new TestResult(true, "PostgreSQL connection successful");

            // Подключаемся к PostgreSQL
            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {

                DatabaseMetaData metaData = conn.getMetaData();

                // Информация о сервере
                result.data.put("databaseProductName", metaData.getDatabaseProductName());
                result.data.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                result.data.put("driverName", metaData.getDriverName());
                result.data.put("driverVersion", metaData.getDriverVersion());
                result.data.put("url", metaData.getURL());
                result.data.put("userName", metaData.getUserName());

                // Текущая база данных
                result.data.put("currentDatabase", conn.getCatalog());

                // Версия PostgreSQL
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT version()")) {
                    if (rs.next()) {
                        result.data.put("version", rs.getString(1));
                    }
                }

                // Имя сервера и порт
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT inet_server_addr(), inet_server_port()")) {
                    if (rs.next()) {
                        result.data.put("serverAddress", rs.getString(1));
                        result.data.put("serverPort", rs.getInt(2));
                    }
                }

                // Список баз данных
                List<Map<String, Object>> databases = new ArrayList<>();
                String dbQuery = "SELECT datname, pg_database_size(datname) as size " +
                        "FROM pg_database WHERE datistemplate = false ORDER BY datname";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(dbQuery)) {
                    while (rs.next()) {
                        Map<String, Object> dbInfo = new HashMap<>();
                        dbInfo.put("name", rs.getString("datname"));
                        dbInfo.put("size", rs.getLong("size"));
                        dbInfo.put("sizeFormatted", formatBytes(rs.getLong("size")));
                        databases.add(dbInfo);
                    }
                }
                result.data.put("databases", databases);
                result.data.put("databaseCount", databases.size());

                // Список схем в текущей базе
                List<String> schemas = new ArrayList<>();
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata " +
                             "WHERE schema_name NOT IN ('pg_catalog', 'information_schema') " +
                             "ORDER BY schema_name")) {
                    while (rs.next()) {
                        schemas.add(rs.getString("schema_name"));
                    }
                }
                result.data.put("schemas", schemas);

                // Статистика подключений
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT count(*) as connections FROM pg_stat_activity")) {
                    if (rs.next()) {
                        result.data.put("activeConnections", rs.getInt("connections"));
                    }
                }

                // Uptime сервера (если есть доступ)
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT pg_postmaster_start_time()")) {
                    if (rs.next()) {
                        result.data.put("serverStartTime", rs.getTimestamp(1).toString());
                    }
                } catch (SQLException e) {
                    // Может не быть прав
                    result.data.put("serverStartTime", "No permission");
                }

                // Репликация (если настроена)
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT pg_is_in_recovery()")) {
                    if (rs.next()) {
                        boolean isReplica = rs.getBoolean(1);
                        result.data.put("isReplica", isReplica);
                        result.data.put("role", isReplica ? "Replica (Standby)" : "Primary (Master)");
                    }
                } catch (SQLException e) {
                    // Может не быть функции в старых версиях
                }

                return result;

            } catch (SQLException e) {
                return new TestResult(false, "PostgreSQL connection failed: " + e.getMessage());
            }

        } catch (Exception e) {
            return new TestResult(false, "Error: " + e.getMessage());
        }
    }

    // Преобразование postgresql:// в jdbc:postgresql://
    private static String convertToJdbcUrl(String url) {
        // postgresql://user:secret@host:port/database?params
        // -> jdbc:postgresql://host:port/database?user=user&secret=secret&params

        try {
            // Проверяем формат URL
            if (!url.startsWith("postgresql://")) {
                return url; // Возвращаем как есть если не postgresql://
            }

            String cleanUrl = url.substring("postgresql://".length());

            String userInfo = "";
            String hostPart = cleanUrl;
            String existingParams = "";

            // Извлекаем user:secret если есть
            if (cleanUrl.contains("@")) {
                int atIndex = cleanUrl.indexOf("@");
                userInfo = cleanUrl.substring(0, atIndex);
                hostPart = cleanUrl.substring(atIndex + 1);
            }

            // Отделяем существующие параметры если есть
            if (hostPart.contains("?")) {
                int questionIndex = hostPart.indexOf("?");
                existingParams = hostPart.substring(questionIndex + 1);
                hostPart = hostPart.substring(0, questionIndex);
            }

            // Строим JDBC URL
            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://");
            jdbcUrl.append(hostPart);

            // Добавляем параметры
            boolean hasParams = false;

            // Добавляем user и secret если есть
            if (!userInfo.isEmpty()) {
                jdbcUrl.append("?");
                hasParams = true;

                // Разбираем user:secret
                if (userInfo.contains(":")) {
                    int colonIndex = userInfo.indexOf(":");
                    String user = userInfo.substring(0, colonIndex);
                    String secret = userInfo.substring(colonIndex + 1);

                    jdbcUrl.append("user=").append(user);
                    jdbcUrl.append("&").append("pass").append("word=").append(secret);
                } else {
                    // Только user без секрета
                    jdbcUrl.append("user=").append(userInfo);
                }
            }

            // Добавляем существующие параметры
            if (!existingParams.isEmpty()) {
                if (!hasParams) {
                    jdbcUrl.append("?");
                } else {
                    jdbcUrl.append("&");
                }
                jdbcUrl.append(existingParams);
            }

            return jdbcUrl.toString();

        } catch (Exception e) {
            // Если не получилось преобразовать, пробуем добавить jdbc:
            if (!url.startsWith("jdbc:")) {
                return "jdbc:" + url;
            }
            return url;
        }
    }

    // Форматирование размера в человекочитаемый вид
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}