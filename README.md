# PT-Log API
The **PT-Log API** is a Spring Boot application that manages and tracks performance test logs for different projects.  
It integrates with an **Oracle database** to persist projects, test executions, configurations, and analysis notes.

---

## ✨ Features
- 🔍 **Healthcheck** endpoint to verify service availability
- 📂 **Project management** – create, list, and delete projects
- 🧪 **Test logging** – insert structured test logs with automatic prefixes and counters
- ⚙️ **Configuration management** – add pacing and general configurations with automatic numbering
- 📝 **Analysis updates** – update `ANALYS` column for specific test entries
- 🗑️ **Test deletion** – remove individual tests or entire projects
- 📊 **Data retrieval** – fetch all tests for a given project
- 🌍 **CORS enabled** – frontend (React) can interact seamlessly
- 📖 **OpenAPI/Swagger** documentation via annotations
- 🔗 **Connection pool monitoring** – view HikariCP statistics

---

## 📦 Tech Stack
- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAPI / Swagger](https://swagger.io/)
- [Oracle Database](https://www.oracle.com/database/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling
- [SLF4J](http://www.slf4j.org/) for logging

---

## ⚙️ Setup

### 1. Clone the repository
```bash
git clone https://github.com/your-org/ptlog-backend.git
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
- **`GET /healthcheck`** → Check if service is alive
  ```json
  {
    "status": "ok",
    "service": "API Health Check"
  }
  ```

### Projects
- **`GET /populate`** → List all projects  
  Returns: `["Project1", "Project2", ...]`

- **`POST /createProject`** → Create a new project  
  ```json
  {
    "Projekt": "ProjectName"
  }
  ```

- **`DELETE /deleteProject`** → Delete a project and all its tests  
  ```json
  {
    "Projekt": "ProjectName"
  }
  ```

### Test Logs
- **`GET /getData?projekt={name}`** → Fetch all logs for a project  
  Returns formatted test data with columns: `DATUM`, `TYP`, `TESTNAMN`, `SYFTE`, `ANALYS`, `PROJEKT`, `TESTARE`

- **`POST /insert`** → Insert a new test log  
  ```json
  {
    "Datum": "2025-10-07T12:00:00Z",
    "Typ": "Referenstest",
    "Testnamn": "MyTest",
    "Syfte": "Load validation",
    "Projekt": "MyProject",
    "Testare": "Johan"
  }
  ```
  **Test types and prefixes:**
  - `Referenstest` → `REF_`
  - `Belastningstest` → `BEL_`
  - `Utmattningstest` → `UTM_`
  - `Maxtest` → `MAX_`
  - `Skapa` → `SKA_`
  - `Verifikationstest` → `VER_`
  
  Result: `01_REF_MyTest`, `02_BEL_LoadTest`, etc.

- **`DELETE /deleteTest`** → Delete a specific test  
  ```json
  {
    "Projekt": "MyProject",
    "Testnamn": "01_REF_MyTest"
  }
  ```

### Configurations
- **`POST /addKonfig`** → Add pacing configuration  
  ```json
  {
    "PROJEKT": "MyProject",
    "TESTNAMN": "PACING",
    "REQH": "3600",
    "REQS": "1.0",
    "VU": "10",
    "PACING": "360.00",
    "SKRIPT": "MyScript",
    "TESTARE": "Johan"
  }
  ```
  Result: `01_PAC_Konfig`, `02_PAC_Konfig`, etc.

- **`POST /addGenerellKonfig`** → Add general configuration  
  ```json
  {
    "PROJEKT": "MyProject",
    "BESKRIVNING": "Server: Tomcat 9.0, JVM: OpenJDK 11, Memory: 4GB",
    "TESTARE": "Johan"
  }
  ```
  Result: `01_GEN_Konfig`, `02_GEN_Konfig`, etc.

### Analysis
- **`PUT /updateAnalys`** → Update analysis for a test  
  ```json
  {
    "Projekt": "MyProject",
    "Testnamn": "01_REF_MyTest",
    "Analys": "Performance within expected range. No errors detected."
  }
  ```

### Monitoring
- **`GET /dbpool`** → Get database connection pool statistics  
  ```json
  {
    "active": 2,
    "idle": 8,
    "waiting": 0,
    "total": 10,
    "maxPoolSize": 10
  }
  ```

---

## 🏗️ Database Schema

### PTLOG_PROJEKT
```sql
CREATE TABLE PTLOG_PROJEKT (
  NAMN VARCHAR2(100) PRIMARY KEY
);
```

### PTLOG
```sql
CREATE TABLE PTLOG (
  DATUM TIMESTAMP,
  TYP VARCHAR2(50),
  TESTNAMN VARCHAR2(200),
  SYFTE VARCHAR2(500),
  ANALYS CLOB,
  PROJEKT VARCHAR2(100),
  TESTARE VARCHAR2(100)
);
```

---

## 📝 Test Naming Convention

All tests are automatically numbered with a two-digit counter:

| Type | Prefix | Example |
|------|--------|---------|
| Referenstest | `REF_` | `01_REF_Baseline` |
| Belastningstest | `BEL_` | `02_BEL_Load500` |
| Utmattningstest | `UTM_` | `03_UTM_24hours` |
| Maxtest | `MAX_` | `04_MAX_Capacity` |
| Skapa | `SKA_` | `05_SKA_Setup` |
| Verifikationstest | `VER_` | `06_VER_Validation` |
| Pacing Config | `PAC_` | `07_PAC_Konfig` |
| General Config | `GEN_` | `08_GEN_Konfig` |

---

## 🚀 Development

### Build
```bash
./mvnw clean install
```

### Run tests
```bash
./mvnw test
```

### Package
```bash
./mvnw package
```

---

## 📚 API Documentation

OpenAPI documentation is available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---
