# PT-Log API

The **PT-Log API** is a Spring Boot application that manages and tracks performance test logs for different projects.  
It integrates with an **Oracle database** to persist projects, test executions, and analysis notes.

---

## ✨ Features

- 🔍 **Healthcheck** endpoint to verify service availability
- 📂 **Project management** – create and list projects
- 🧪 **Test logging** – insert structured test logs with automatic prefixes and counters
- 📝 **Analysis updates** – update `ANALYS` column for specific test entries
- 📊 **Data retrieval** – fetch all tests for a given project
- 🌍 **CORS enabled** – frontend (React) can interact seamlessly
- 📖 **OpenAPI/Swagger** documentation via annotations

---

## 📦 Tech Stack

- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAPI / Swagger](https://swagger.io/)
- [Oracle Database](https://www.oracle.com/database/)
- [SLF4J](http://www.slf4j.org/) for logging

---

## ⚙️ Setup

### 1. Clone the repository
```bash
git clone https://github.com/ostbergjohan/pt-log-backend.git
cd ptlog-backend
```

### 2. Configure Database
Update `application.properties` with your Oracle DB connection:
```properties
spring.datasource.url=jdbc:oracle:thin:@ldap://your-ldap-server:389/your-db
spring.datasource.username=your-username
spring.datasource.password=your-password
```

### 3. Run the application
```bash
./mvnw spring-boot:run
```

The API will be available at:  
[http://localhost:8080](http://localhost:8080)

---

## 🔌 API Endpoints

### Health
- `GET /healthcheck` → check if service is alive

### Projects
- `GET /populate` → list all projects  
- `POST /createProject`  
  ```json
  { "Projekt": "ProjectName" }
  ```

### Test Logs
- `GET /getData?projekt={name}` → fetch logs for a project  
- `POST /insert`  
  ```json
  {
    "Datum": "2025-08-28T12:00:00Z",
    "Typ": "Referenstest",
    "Testnamn": "MyTest",
    "Syfte": "Load validation",
    "Projekt": "MyProject",
    "Testare": "Johan"
  }
  ```

### Analysis
- `PUT /updateAnalys`  
  ```json
  {
    "Projekt": "MyProject",
    "Testnamn": "01_REF_MyTest",
    "Analys": "Performance within expected range."
  }
  ```

---
