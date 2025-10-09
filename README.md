# PT-Log API
The **PT-Log API** is a Spring Boot application that manages and tracks performance test logs for different projects.  
It supports both **H2 file-based database** (for development/local deployments) and **Oracle database** (for production environments).

This is the backend API for the [PT-Log Frontend](https://github.com/ostbergjohan/pt-log-front/).

---

## ✨ Features
- 🔍 **Healthcheck** endpoint to verify service availability
- 💾 **Dual database support** – H2 (file-based) or Oracle with simple configuration
- 📂 **Project management** – create, list, archive, restore, and delete projects
- 🗄️ **Archive system** – archive projects for later reference while keeping them accessible
- 🧪 **Test logging** – insert structured test logs with automatic prefixes and counters
- ⚙️ **Configuration management** – add pacing and general configurations with automatic numbering
- 📝 **Analysis updates** – update `ANALYS` column for specific test entries
- 🗑️ **Test deletion** – remove individual tests or entire projects
- 📊 **Data retrieval** – fetch all tests for a given project (active or archived)
- 📖 **OpenAPI/Swagger** documentation via annotations
- 📡 **Elastic APM** integration for monitoring
- 🔗 **Connection pool monitoring** – view HikariCP statistics
- 🗄️ **H2 Web Console** – built-in database browser (development mode)

---

## 📦 Tech Stack
- [Spring Boot](https://spring.io/projects/spring-boot) 3.4.4
- [H2 Database](https://h2database.com/) 2.2.224 (embedded)
- [Oracle Database](https://www.oracle.com/database/) (optional)
- [OpenAPI / Swagger](https://swagger.io/)
- [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling
- [Elastic APM](https://www.elastic.co/apm)
- [SLF4J](http://www.slf4j.org/) for logging

---

## ⚙️ Setup

### 1. Clone the repository
```bash
git clone https://github.com/your-org/ptlog-backend.git
cd ptlog-backend
```

### 2. Configure Database

#### Option A: H2 (Local/Development - Default)
Update `application.properties`:
```properties
# Use H2 file-based database
db.type=h2
h2.file.path=./data/ptlog
h2.auto.init=true
h2.username=sa
h2.password=

# Enable H2 web console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

The H2 database file will be created automatically at `./data/ptlog.mv.db`

**Access H2 Console:**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/ptlog`
- Username: `sa`
- Password: *(leave empty)*

#### Option B: Oracle (Production)
Update `application.properties`:
```properties
# Use Oracle database
db.type=oracle
spring.datasource.url=jdbc:oracle:thin:@your-server:1521:ORCL
spring.datasource.username=your-username
spring.datasource.password=your-password
oracle.auto.init=false

# Disable H2 console
spring.h2.console.enabled=false
```

### 3. Run the application

**Using Maven:**
```bash
./mvnw spring-boot:run
```

**Using Docker:**
```bash
# Build image
docker build -t ptlog:latest .

# Run with H2
docker run -d -p 8080:8080 \
  -v ptlog-data:/opt/app/data \
  -e db.type=h2 \
  ptlog:latest

# Run with Oracle
docker run -d -p 8080:8080 \
  -e db.type=oracle \
  -e spring.datasource.url=jdbc:oracle:thin:@host:1521:ORCL \
  -e spring.datasource.username=user \
  -e spring.datasource.password=pass \
  ptlog:latest
```

The API will be available at:  
[http://localhost:8080](http://localhost:8080)

---

## 📌 API Endpoints

### Health
- **`GET /healthcheck`** → Check if service is alive
  ```json
  {
    "status": "ok",
    "service": "API Health Check"
  }
  ```

### Projects (Active)
- **`GET /populate`** → List all active projects (WHERE ARKIVERAD = 0)  
  Returns: `["Project1", "Project2", ...]`

- **`POST /createProject`** → Create a new project  
  ```json
  {
    "Projekt": "ProjectName"
  }
  ```

- **`GET /getData?projekt={name}`** → Fetch all logs for a project  
  Returns formatted test data with columns: `DATUM`, `TYP`, `TESTNAMN`, `SYFTE`, `ANALYS`, `PROJEKT`, `TESTARE`

### Projects (Archived)
- **`GET /populateArkiverade`** → List all archived projects (WHERE ARKIVERAD = 1)  
  Returns: `["ArchivedProject1", "ArchivedProject2", ...]`

- **`POST /arkivera?namn={projectName}`** → Archive a project (set ARKIVERAD = 1)  
  Archives the project, hiding it from the active list while keeping all data

- **`POST /restore?namn={projectName}`** → Restore an archived project (set ARKIVERAD = 0)  
  Restores the project back to the active list

- **`DELETE /deleteProject`** → Permanently delete a project and all its tests  
  ```json
  {
    "Projekt": "ProjectName"
  }
  ```

### Test Logs
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

## 🗂️ Database Schema

The application automatically creates the required schema on first run when using H2 (`h2.auto.init=true`) or Oracle (`oracle.auto.init=true`).

### PTLOG_PROJEKT
**H2:**
```sql
CREATE TABLE PTLOG_PROJEKT (
  NAMN VARCHAR(255) PRIMARY KEY,
  ARKIVERAD INT DEFAULT 0 NOT NULL CHECK (ARKIVERAD IN (0, 1))
);
```

**Oracle:**
```sql
CREATE TABLE PTLOG_PROJEKT (
  NAMN VARCHAR2(255) PRIMARY KEY,
  ARKIVERAD NUMBER(1) DEFAULT 0 NOT NULL CHECK (ARKIVERAD IN (0, 1))
);
```

### PTLOG
**H2:**
```sql
CREATE TABLE PTLOG (
  ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  DATUM TIMESTAMP NOT NULL,
  TYP VARCHAR(50) NOT NULL,
  TESTNAMN VARCHAR(255) NOT NULL,
  SYFTE VARCHAR(1000),
  ANALYS CLOB,
  PROJEKT VARCHAR(255) NOT NULL,
  TESTARE VARCHAR(255),
  CONSTRAINT FK_PROJEKT FOREIGN KEY (PROJEKT) 
    REFERENCES PTLOG_PROJEKT(NAMN) ON DELETE CASCADE
);
```

**Oracle:**
```sql
CREATE SEQUENCE PTLOG_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE PTLOG (
  ID NUMBER DEFAULT PTLOG_SEQ.NEXTVAL PRIMARY KEY,
  DATUM TIMESTAMP NOT NULL,
  TYP VARCHAR2(50) NOT NULL,
  TESTNAMN VARCHAR2(255) NOT NULL,
  SYFTE VARCHAR2(1000),
  ANALYS CLOB,
  PROJEKT VARCHAR2(255) NOT NULL,
  TESTARE VARCHAR2(255),
  CONSTRAINT FK_PROJEKT FOREIGN KEY (PROJEKT) 
    REFERENCES PTLOG_PROJEKT(NAMN) ON DELETE CASCADE
);
```

### Indexes (Oracle)
```sql
CREATE INDEX IDX_PTLOG_PROJEKT ON PTLOG(PROJEKT);
CREATE INDEX IDX_PTLOG_DATUM ON PTLOG(DATUM DESC);
CREATE INDEX IDX_PTLOG_TESTNAMN ON PTLOG(TESTNAMN);
CREATE INDEX IDX_PTLOG_PROJEKT_ARKIVERAD ON PTLOG_PROJEKT(ARKIVERAD);
```

---

## 🗄️ Archive System

The archive system allows projects to be hidden from the main view while preserving all data:

- **Archive Status**: Stored in `ARKIVERAD` column (0 = active, 1 = archived)
- **Active Projects**: `WHERE ARKIVERAD = 0`
- **Archived Projects**: `WHERE ARKIVERAD = 1`
- **All Tests Preserved**: Archiving only affects project visibility, not test data
- **Reversible**: Projects can be restored from archive at any time

**Archive Workflow:**
1. Archive a project → Sets `ARKIVERAD = 1`
2. Project disappears from `/populate` endpoint
3. Project appears in `/populateArkiverade` endpoint
4. All tests remain accessible via `/getData?projekt={name}`
5. Restore project → Sets `ARKIVERAD = 0`
6. Project returns to active list

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

### Docker Build
```bash
# Build image
podman build -t ptlog:latest .

# Tag for registry
podman tag ptlog:latest your-registry/ptlog:latest

# Push to registry
podman push your-registry/ptlog:latest
```

---

## 🐳 Docker Deployment

### Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `db.type` | Database type: `h2` or `oracle` | `h2` | No |
| `h2.file.path` | H2 database file path | `./data/ptlog` | No |
| `h2.auto.init` | Auto-create H2 schema | `true` | No |
| `spring.h2.console.enabled` | Enable H2 web console | `true` | No |
| `spring.datasource.url` | Oracle JDBC URL | - | Yes (Oracle) |
| `spring.datasource.username` | Database username | `sa` (H2) | Yes (Oracle) |
| `spring.datasource.password` | Database password | - | Yes (Oracle) |
| `oracle.auto.init` | Auto-create Oracle schema | `false` | No |

### Docker Compose Example

```yaml
version: '3.8'

services:
  ptlog:
    image: ptlog:latest
    ports:
      - "8080:8080"
    environment:
      - db.type=h2
      - h2.file.path=/opt/app/data/ptlog
      - spring.h2.console.enabled=true
    volumes:
      - ptlog-data:/opt/app/data
    restart: unless-stopped

volumes:
  ptlog-data:
```

---

## 💾 Data Persistence & Backup

### H2 Database

**Backup:**
```bash
# Stop application
docker stop ptlog

# Copy database file
docker cp ptlog:/opt/app/data/ptlog.mv.db ./backup/ptlog-$(date +%Y%m%d).mv.db

# Or backup the volume
docker run --rm -v ptlog-data:/data -v $(pwd)/backup:/backup \
  alpine tar czf /backup/ptlog-backup.tar.gz -C /data .
```

**Restore:**
```bash
# Copy database file back
docker cp ./backup/ptlog-20250107.mv.db ptlog:/opt/app/data/ptlog.mv.db

# Restart application
docker start ptlog
```

### Migration Between Databases

**From H2 to Oracle:**
1. Export H2 data (use H2 console or SQL scripts)
2. Update `application.properties` to use Oracle
3. Set `oracle.auto.init=true` for first run
4. Import data to Oracle
5. Set `oracle.auto.init=false`

**From Oracle to H2:**
1. Export Oracle data
2. Update `application.properties` to use H2
3. Application will auto-create H2 schema
4. Import data to H2

---

## 📚 API Documentation

OpenAPI documentation is available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

## 🔒 Security Notes

- **Database credentials**: Store securely using environment variables or secrets management
- **CORS**: Currently set to `origins = "*"` – restrict this in production
- **H2 Console**: Disable in production by setting `spring.h2.console.enabled=false`
- **Authentication**: Consider adding authentication/authorization for production deployments
- **File permissions**: Ensure H2 data directory has proper permissions (755 recommended)

---

## 🛠 Troubleshooting

### H2 Database Issues

**"Database file is locked"**
- Only one application instance can access the H2 file
- Check for running instances: `ps aux | grep ptlog`

**"Schema not initialized"**
- Verify `h2.auto.init=true` in `application.properties`
- Check logs for initialization errors
- Delete database file and restart: `rm ./data/ptlog.mv.db`

**Cannot access H2 Console**
- Ensure `spring.h2.console.enabled=true`
- Verify URL: `http://localhost:8080/h2-console`
- JDBC URL must be: `jdbc:h2:file:./data/ptlog`

### Oracle Database Issues

**Connection timeout**
- Verify Oracle server is accessible
- Check firewall rules
- Validate JDBC URL format

**Schema not created**
- Set `oracle.auto.init=true` for first run
- Check user has CREATE TABLE permissions

### Docker Issues

**Volume permissions**
- Ensure `/opt/app/data` is writable by user 1001
- Check: `docker exec ptlog ls -la /opt/app/data`

**Database not persisting**
- Verify volume is mounted: `docker inspect ptlog`
- Check volume exists: `docker volume ls`

### Archive Issues

**Archived projects not appearing**
- Verify `ARKIVERAD` column exists in `PTLOG_PROJEKT`
- Check query: `SELECT * FROM PTLOG_PROJEKT WHERE ARKIVERAD = 1`
- Ensure `/populateArkiverade` endpoint is implemented

**Cannot restore project**
- Verify `/restore` endpoint accepts POST with `namn` parameter
- Check project exists: `SELECT * FROM PTLOG_PROJEKT WHERE NAMN = ?`

---

## 🎯 Quick Start Guide

### Development (H2)
```bash
# 1. Clone repo
git clone https://github.com/your-org/ptlog-backend.git
cd ptlog-backend

# 2. Run with H2 (default configuration)
./mvnw spring-boot:run

# 3. Access application
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

### Production (Oracle)
```bash
# 1. Update application.properties
db.type=oracle
spring.datasource.url=jdbc:oracle:thin:@prod-server:1521:ORCL
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# 2. Build and run
./mvnw clean package
java -jar target/PtLog-0.0.1-SNAPSHOT.jar
```

### Docker Production
```bash
# 1. Build image
docker build -t ptlog:latest .

# 2. Run with Oracle
docker run -d -p 8080:8080 \
  -e db.type=oracle \
  -e spring.datasource.url=jdbc:oracle:thin:@prod:1521:ORCL \
  -e spring.datasource.username=user \
  -e spring.datasource.password=pass \
  --name ptlog \
  ptlog:latest
```


---

## 🔗 Related Projects

- **Frontend**: [pt-log-front](https://github.com/ostbergjohan/pt-log-front)

---

**Built with ❤️ using Spring Boot and H2 Database**