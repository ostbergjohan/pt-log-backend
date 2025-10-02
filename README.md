# PT-Log API

PT-Log is a Spring Boot–based API to **manage and track performance test logs**.  
It uses an Oracle database with connection pooling via **HikariCP** and includes monitoring endpoints.  

---

## 🚀 Features
- REST API for test project and log management
- Database-backed with transaction support
- OpenAPI/Swagger annotations
- HikariCP database pool monitoring
- Built-in CORS support

---

## 📖 API Endpoints

### Health
- **GET `/healthcheck`**  
  Returns API health status.

---

### Project Management
- **GET `/populate`**  
  List all projects.  

- **POST `/createProject`**  
  Create a new project.  

- **DELETE `/deleteProject`**  
  Delete a project and all its associated test logs.  

---

### Test Logs
- **GET `/getData?projekt={projekt}`**  
  Retrieve logs for a given project.  

- **POST `/insert`**  
  Insert a new test log entry.  
  - Automatically generates a prefixed `TESTNAMN` based on test type.  

- **POST `/addKonfig`**  
  Add a configuration (`KONFIG`) entry.  

- **PUT `/updateAnalys`**  
  Update analysis field for a given test.  

- **DELETE `/deleteTest`**  
  Delete a specific test log.  

---

### Monitoring
- **GET `/dbpool`**  
  Returns HikariCP connection pool statistics:  

```json
{
  "active": 2,
  "idle": 8,
  "waiting": 0,
  "total": 10,
  "maxPoolSize": 20
}
