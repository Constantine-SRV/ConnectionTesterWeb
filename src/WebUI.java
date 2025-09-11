public class WebUI {

    public static String getHTML() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Database Connection Tester</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container { 
                        max-width: 900px; 
                        margin: 0 auto; 
                    }
                    .header {
                        text-align: center;
                        color: white;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        font-size: 2.5em;
                        margin-bottom: 10px;
                        text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
                    }
                    .card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 20px;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                    }
                    .db-section {
                        margin-bottom: 30px;
                        padding-bottom: 30px;
                        border-bottom: 2px solid #f0f0f0;
                    }
                    .db-section:last-child {
                        border-bottom: none;
                        margin-bottom: 0;
                        padding-bottom: 0;
                    }
                    .db-header {
                        display: flex;
                        align-items: center;
                        margin-bottom: 15px;
                    }
                    .db-icon {
                        font-size: 2em;
                        margin-right: 15px;
                    }
                    .db-title {
                        font-size: 1.5em;
                        font-weight: 600;
                        color: #333;
                    }
                    .mongo { color: #4DB33D; }
                    .postgres { color: #336791; }
                    
                    input { 
                        width: 100%; 
                        padding: 12px 15px; 
                        margin: 10px 0; 
                        border: 2px solid #e0e0e0; 
                        border-radius: 8px; 
                        font-size: 14px;
                        transition: border-color 0.3s;
                    }
                    input:focus {
                        outline: none;
                        border-color: #667eea;
                    }
                    .button-group {
                        display: flex;
                        gap: 10px;
                        margin-top: 15px;
                    }
                    button { 
                        flex: 1;
                        padding: 12px 24px; 
                        border: none; 
                        border-radius: 8px; 
                        cursor: pointer; 
                        font-size: 15px;
                        font-weight: 600;
                        transition: all 0.3s;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .btn-mongo { 
                        background: linear-gradient(135deg, #4DB33D, #3d8b31);
                        color: white;
                    }
                    .btn-mongo:hover { 
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(77, 179, 61, 0.3);
                    }
                    .btn-postgres { 
                        background: linear-gradient(135deg, #336791, #265073);
                        color: white;
                    }
                    .btn-postgres:hover { 
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(51, 103, 145, 0.3);
                    }
                    
                    #result { 
                        margin-top: 20px; 
                        padding: 20px; 
                        border-radius: 10px; 
                        display: none;
                        animation: slideIn 0.3s ease-out;
                    }
                    @keyframes slideIn {
                        from { opacity: 0; transform: translateY(-10px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .success { 
                        background: linear-gradient(135deg, #d4edda, #c3e6cb); 
                        border: 2px solid #28a745;
                        color: #155724; 
                    }
                    .error { 
                        background: linear-gradient(135deg, #f8d7da, #f5c6cb); 
                        border: 2px solid #dc3545;
                        color: #721c24; 
                    }
                    .loading {
                        background: linear-gradient(135deg, #d1ecf1, #bee5eb);
                        border: 2px solid #17a2b8;
                        color: #0c5460;
                    }
                    pre { 
                        background: #f8f9fa; 
                        padding: 15px; 
                        border-radius: 8px; 
                        overflow-x: auto;
                        border: 1px solid #dee2e6;
                        margin-top: 10px;
                        font-size: 13px;
                        line-height: 1.5;
                    }
                    .example { 
                        color: #6c757d; 
                        font-size: 12px; 
                        margin-top: 5px;
                        font-style: italic;
                    }
                    .status-icon {
                        display: inline-block;
                        margin-right: 8px;
                        font-size: 1.2em;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔧 Database Connection Tester V-5</h1>
                        <p>Test your MongoDB and PostgreSQL connections</p>
                    </div>
                    
                    <div class="card">
                        <!-- MongoDB Section -->
                        <div class="db-section">
                            <div class="db-header">
                                <span class="db-icon mongo">🍃</span>
                                <span class="db-title">MongoDB</span>
                            </div>
                            <input type="text" id="mongoConnection" 
                                   placeholder="mongodb://host:port/database?replicaSet=rs0" 
                                   value="mongodb://192.168.55.30:27017,192.168.55.40:27017,192.168.0.60:27017/admin?replicaSet=rs0">
                            <div class="example">Example: mongodb://host:27017/admin или полный URL с credentials</div>
                            
                            <div style="display: flex; gap: 10px; margin-top: 10px;">
                                <input type="text" id="mongoUser" placeholder="Username" value="mongoAdmin" style="flex: 1;">
                                <input type="text" id="mongoPass" placeholder="Secret" value="" style="flex: 1;">
                            </div>
                            
                            <input type="text" id="mongoCommand" 
                                   placeholder="Command to execute (JSON format)" 
                                   value='{"hello": 1}' 
                                   style="margin-top: 10px;">
                            <div class="example">Examples: {"hello": 1}, {"ping": 1}, {"listDatabases": 1}, {"serverStatus": 1}</div>
                            
                            <div class="button-group">
                                <button class="btn-mongo" onclick="testMongo()">
                                    🔍 Test MongoDB
                                </button>
                            </div>
                        </div>
                        
                        <!-- PostgreSQL Section -->
                        <div class="db-section">
                            <div class="db-header">
                                <span class="db-icon postgres">🐘</span>
                                <span class="db-title">PostgreSQL</span>
                            </div>
                            <input type="text" id="postgresConnection" 
                                   placeholder="postgresql://host:port/database или jdbc:postgresql://host:port/database" 
                                   value="postgresql://localhost:5432/postgres">
                            <div class="example">Examples: postgresql://localhost:5432/dbname или jdbc:postgresql://localhost:5432/dbname</div>
                            
                            <div style="display: flex; gap: 10px; margin-top: 10px;">
                                <input type="text" id="postgresUser" placeholder="Username" value="postgres" style="flex: 1;">
                                <input type="text" id="postgresPass" placeholder="Secret" value="" style="flex: 1;">
                            </div>
                            <div class="example">Оставьте поля пустыми если используете полный URL с credentials</div>
                            
                            <div class="button-group">
                                <button class="btn-postgres" onclick="testPostgres()">
                                    🔍 Test PostgreSQL
                                </button>
                            </div>
                        </div>
                        
                        <div id="result"></div>
                    </div>
                </div>
                
                <script>
                    async function testMongo() {
                        const conn = document.getElementById('mongoConnection').value;
                        const user = document.getElementById('mongoUser').value;
                        const pass = document.getElementById('mongoPass').value;
                        const command = document.getElementById('mongoCommand').value;
                        
                        let url = '/api/test/mongo?connection=' + encodeURIComponent(conn);
                        if (user) {
                            url += '&user=' + encodeURIComponent(user);
                        }
                        if (pass) {
                            url += '&secret=' + encodeURIComponent(pass);
                        }
                        if (command) {
                            url += '&command=' + encodeURIComponent(command);
                        }
                        
                        await testConnection('mongo', url, true);
                    }
                    
                    async function testPostgres() {
                        const conn = document.getElementById('postgresConnection').value;
                        const user = document.getElementById('postgresUser').value;
                        const pass = document.getElementById('postgresPass').value;
                        
                        let url = '/api/test/postgres?connection=' + encodeURIComponent(conn);
                        if (user) {
                            url += '&user=' + encodeURIComponent(user);
                        }
                        if (pass) {
                            url += '&secret=' + encodeURIComponent(pass);
                        }
                        
                        await testConnection('postgres', url, true);
                    }
                    
                    async function testConnection(type, urlOrConnection, isFullUrl = false) {
                        const resultDiv = document.getElementById('result');
                        
                        resultDiv.innerHTML = '<span class="status-icon">⏳</span>Testing ' + type.toUpperCase() + ' connection...';
                        resultDiv.className = 'loading';
                        resultDiv.style.display = 'block';
                        
                        try {
                            let fetchUrl;
                            if (isFullUrl) {
                                fetchUrl = urlOrConnection;
                            } else {
                                fetchUrl = '/api/test/' + type + '?connection=' + encodeURIComponent(urlOrConnection);
                            }
                            
                            const response = await fetch(fetchUrl);
                            const data = await response.json();
                            
                            if (data.success) {
                                resultDiv.className = 'success';
                                resultDiv.innerHTML = '<span class="status-icon">✅</span><strong>' + data.message + '</strong>' +
                                                    '<pre>' + JSON.stringify(data.data, null, 2) + '</pre>';
                            } else {
                                resultDiv.className = 'error';
                                resultDiv.innerHTML = '<span class="status-icon">❌</span><strong>Connection Failed</strong><br>' + 
                                                    data.message;
                            }
                        } catch (e) {
                            resultDiv.className = 'error';
                            resultDiv.innerHTML = '<span class="status-icon">❌</span><strong>Request Error:</strong> ' + e.message;
                        }
                    }
                    
                    // Enter key support
                    document.getElementById('mongoConnection').addEventListener('keypress', function(e) {
                        if (e.key === 'Enter') testMongo();
                    });
                    document.getElementById('postgresConnection').addEventListener('keypress', function(e) {
                        if (e.key === 'Enter') testPostgres();
                    });
                </script>
            </body>
            </html>
            """;
    }
}