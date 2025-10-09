package se.ptlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

import com.zaxxer.hikari.HikariDataSource;

@OpenAPIDefinition(
        info = @Info(
                title = "PT-Log",
                version = "v2.0",
                description = "PT-Log API to manage and track tests.\n" +
                        "Endpoints:\n" +
                        "**Health**\n" +
                        "1. **GET /healthcheck** - API health check.\n\n" +
                        "**Active Projects**\n" +
                        "2. **GET /populate** - List active projects (ARKIVERAD = 0).\n" +
                        "3. **GET /getData?projekt={projekt}** - Retrieve test logs for a project.\n" +
                        "4. **POST /createProject** - Create new project.\n" +
                        "5. **POST /insert** - Insert test log.\n" +
                        "6. **PUT /updateAnalys** - Update analysis for a test.\n" +
                        "7. **POST /addKonfig** - Add pacing configuration.\n" +
                        "8. **POST /addGenerellKonfig** - Add general configuration.\n" +
                        "9. **DELETE /deleteTest** - Delete specific test.\n\n" +
                        "**Archived Projects**\n" +
                        "10. **GET /populateArkiverade** - List archived projects (ARKIVERAD = 1).\n" +
                        "11. **POST /arkivera?namn={namn}** - Archive a project.\n" +
                        "12. **POST /restore?namn={namn}** - Restore archived project.\n" +
                        "13. **DELETE /deleteProject** - Permanently delete project and all its tests.\n\n" +
                        "**Monitoring**\n" +
                        "14. **GET /dbpool** - Database connection pool statistics.\n"
        )
)
@SpringBootApplication(
        scanBasePackages = "se.ptlog",
        exclude = {DataSourceAutoConfiguration.class}  // Disable auto-config, use our DatabaseConfig
)
@RestController
@EnableScheduling
public class PtLog {

    private static final Logger logger = LoggerFactory.getLogger(PtLog.class);
    private final DataSource dataSource;

    // ✅ Constructor injection ensures dataSource is not null
    public PtLog(DataSource dataSource) {
        this.dataSource = dataSource;
        logger.info("🚀 PtLog initialized with DataSource: {}", dataSource.getClass().getName());
    }

    public static void main(String[] args) {
        SpringApplication.run(PtLog.class, args);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("healthcheck")
    public ResponseEntity<String> healthcheck() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        return ResponseEntity.ok()
                .headers(headers)
                .body("{\"status\":\"ok\",\"service\":\"API Health Check\"}");
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/getData")
    public List<Map<String, Object>> getData(@RequestParam String projekt) throws SQLException {
        String sql = "SELECT " +
                "TO_CHAR(DATUM, 'YYYY-MM-DD HH24:MI') AS DATUM, " +
                "TYP, TESTNAMN, SYFTE, ANALYS, PROJEKT, TESTARE " +
                "FROM ptlog WHERE PROJEKT = ? ORDER BY DATUM DESC";
        return OraSQL(sql, projekt);
    }

    public List<Map<String, Object>> OraSQL(String query, String... params) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement st = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                st.setString(i + 1, params[i]);
            }

            try (ResultSet rs = st.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int colCount = rsmd.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);

                        // Convert CLOB to String for JSON serialization (H2 compatibility)
                        if (value instanceof java.sql.Clob) {
                            java.sql.Clob clob = (java.sql.Clob) value;
                            value = clob.getSubString(1, (int) clob.length());
                        }

                        row.put(rsmd.getColumnName(i), value);
                    }
                    resultList.add(row);
                }
            }
        }
        return resultList;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/restore")
    public void restoreProjekt(@RequestParam String namn) throws SQLException {
        String sql = "UPDATE PTLOG_PROJEKT SET ARKIVERAD = 0 WHERE NAMN = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, namn);
            pstmt.executeUpdate();
        }
    }
    @CrossOrigin(origins = "*")
    @GetMapping("/populate")
    public List<String> getAllProjekts() throws SQLException {
        String sql = "SELECT DISTINCT NAMN " +
                "FROM PTLOG_PROJEKT " +
                "WHERE ARKIVERAD = 0 " +
                "ORDER BY NAMN";
        List<String> projekts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                projekts.add(rs.getString("NAMN"));
            }
        }
        return projekts;
    }
    @CrossOrigin(origins = "*")
    @PostMapping("/arkivera")
    public void arkiveraProjekt(@RequestParam String namn) throws SQLException {
        String sql = "UPDATE PTLOG_PROJEKT SET ARKIVERAD = 1 WHERE NAMN = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, namn);
            pstmt.executeUpdate();
        }
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/populateArkiverade")
    public List<String> getArkiveradeProjekts() throws SQLException {
        String sql = "SELECT DISTINCT NAMN " +
                "FROM PTLOG_PROJEKT " +
                "WHERE ARKIVERAD = 1 " +
                "ORDER BY NAMN";
        List<String> projekts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                projekts.add(rs.getString("NAMN"));
            }
        }
        return projekts;
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping("/deleteProject")
    public ResponseEntity<String> deleteProject(@RequestBody Map<String, String> payload) {
        String projektName = payload.get("Projekt");
        if (projektName == null || projektName.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Projekt name is required");
        }

        String deletePTLOGSql = "DELETE FROM PTLOG WHERE PROJEKT = ?";
        String deleteProjektSql = "DELETE FROM PTLOG_PROJEKT WHERE NAMN = ?";

        try (Connection conn = dataSource.getConnection()) {
            // Use transaction
            conn.setAutoCommit(false);

            int deletedRowsPTLOG;
            try (PreparedStatement stmt = conn.prepareStatement(deletePTLOGSql)) {
                stmt.setString(1, projektName.trim());
                deletedRowsPTLOG = stmt.executeUpdate();
            }

            int deletedRowsProjekt;
            try (PreparedStatement stmt = conn.prepareStatement(deleteProjektSql)) {
                stmt.setString(1, projektName.trim());
                deletedRowsProjekt = stmt.executeUpdate();
            }

            conn.commit();

            if (deletedRowsProjekt == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No project found with name: " + projektName);
            }

            return ResponseEntity.ok("Deleted project: " + projektName +
                    " (" + deletedRowsProjekt + " row(s)) and " +
                    deletedRowsPTLOG + " associated PTLOG row(s)");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/createProject")
    public ResponseEntity<String> createProject(@RequestBody Map<String, String> payload) {
        String projektName = payload.get("Projekt");
        if (projektName == null || projektName.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Projekt name is required");
        }

        String sql = "INSERT INTO PTLOG_PROJEKT (NAMN) VALUES (?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projektName.trim());
            int rows = stmt.executeUpdate();
            return ResponseEntity.ok("Inserted project: " + projektName + " (" + rows + " row(s))");
        } catch (SQLException e) {
            logger.error("Database error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    public int countRowsForProject(String projekt) throws SQLException {
        String sql = "SELECT COUNT(*) AS CNT FROM ptlog WHERE PROJEKT = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projekt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("CNT");
                    logger.info("Project '{}' has {} existing rows", projekt, count);
                    return count;
                }
            }
        }
        logger.info("Project '{}' has 0 rows", projekt);
        return 0;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/insert")
    public ResponseEntity<String> insertLog(@RequestBody String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String datum, typ, testnamn, syfte, projekt, testare;
        try {
            datum = getRequiredField(node, "Datum");
            typ = getRequiredField(node, "Typ");
            testnamn = getRequiredField(node, "Testnamn");
            syfte = getRequiredField(node, "Syfte");
            projekt = getRequiredField(node, "Projekt");
            testare = getRequiredField(node, "Testare");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required field: " + e.getMessage());
        }

        int count;
        try {
            count = countRowsForProject(projekt);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to count rows: " + e.getMessage());
        }

        String counterStr = String.format("%02d", count + 1);
        String prefix = getTestTypePrefix(typ);
        testnamn = counterStr + "_" + prefix + "_" + testnamn;

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"));

        String sql = "INSERT INTO PTLOG (DATUM, TYP, TESTNAMN, SYFTE, PROJEKT, TESTARE) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(Instant.parse(datum)));
            stmt.setString(2, typ);
            stmt.setString(3, testnamn);
            stmt.setString(4, syfte);
            stmt.setString(5, projekt);
            stmt.setString(6, testare);

            int rows = stmt.executeUpdate();
            logger.info("Inserted test: {} for project: {}", testnamn, projekt);
            return ResponseEntity.ok("Inserted " + rows + " row(s) with testnamn: " + testnamn);
        } catch (SQLException e) {
            logger.error("Failed to insert test: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    private String getTestTypePrefix(String typ) {
        // Support both English keys and Swedish full names
        switch (typ.toLowerCase()) {
            case "reference":
            case "referenstest":
                return "REF";
            case "verification":
            case "verifikationstest":
                return "VER";
            case "load":
            case "belastningstest":
                return "BEL";
            case "endurance":
            case "utmattningstest":
                return "UTM";
            case "max":
            case "maxtest":
                return "MAX";
            case "create":
            case "skapa":
                return "SKA";
            default:
                logger.warn("Unknown test type: {}, defaulting to REF", typ);
                return "REF";
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/addKonfig")
    public ResponseEntity<String> addKonfig(@RequestBody String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String projekt, reqH, reqS, vu, pacing, skript, testare, testnamn;
        try {
            projekt = getRequiredField(node, "PROJEKT");
            testnamn = node.has("TESTNAMN") ? node.get("TESTNAMN").asText() : "PACING";
            reqH = node.get("REQH").asText();
            reqS = node.get("REQS").asText();
            vu = node.get("VU").asText();
            pacing = node.get("PACING").asText();
            skript = node.has("SKRIPT") ? node.get("SKRIPT").asText() : "";
            testare = node.has("TESTARE") ? node.get("TESTARE").asText() : "";
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields: " + e.getMessage());
        }

        // Get counter for this project
        int count;
        try {
            count = countRowsForProject(projekt);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to count rows: " + e.getMessage());
        }

        String counterStr = String.format("%02d", count + 1);
        // Determine config type based on testnamn (PACING stays as PACING, others default to PAC)
        String configType = testnamn.equalsIgnoreCase("PACING") ? "PAC" : "PAC";
        String finalTestnamn = counterStr + "_" + configType + "_" +
                (testnamn.equalsIgnoreCase("KONFIG") ? "Konfig" :
                        testnamn.equalsIgnoreCase("CONFIG") ? "Config" : testnamn);

        // Format data for ANALYS field
        String analys = String.format("ReqH: %s | ReqS: %s | VU: %s | Pacing: %s | Skript: %s",
                reqH, reqS, vu, pacing, skript);

        // Determine TYP field based on language
        String typ = testnamn.equalsIgnoreCase("CONFIG") ? "CONFIG" : "KONFIG";

        String sql = "INSERT INTO PTLOG (DATUM, TYP, TESTNAMN, SYFTE, ANALYS, PROJEKT, TESTARE) VALUES (?, ?, ?, ?, ?, ?, ?)";

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, typ);
            stmt.setString(3, finalTestnamn);
            stmt.setString(4, typ); // Use same value for SYFTE
            stmt.setString(5, analys);
            stmt.setString(6, projekt);
            stmt.setString(7, testare);

            stmt.executeUpdate();
            logger.info("Inserted {}: {} for project: {}", typ, finalTestnamn, projekt);
            return ResponseEntity.ok(typ + " added successfully with testnamn: " + finalTestnamn);
        } catch (SQLException e) {
            logger.error("Failed to insert {}: {}", typ, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/addGenerellKonfig")
    public ResponseEntity<String> addGenerellKonfig(@RequestBody String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String projekt, beskrivning, testare, testnamn;
        try {
            projekt = getRequiredField(node, "PROJEKT");
            beskrivning = getRequiredField(node, "BESKRIVNING");
            testare = node.has("TESTARE") ? node.get("TESTARE").asText() : "";
            testnamn = node.has("TESTNAMN") ? node.get("TESTNAMN").asText() : "KONFIG";
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required fields: " + e.getMessage());
        }

        // Get counter for this project
        int count;
        try {
            count = countRowsForProject(projekt);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to count rows: " + e.getMessage());
        }

        String counterStr = String.format("%02d", count + 1);
        String finalTestnamn = counterStr + "_GEN_" +
                (testnamn.equalsIgnoreCase("CONFIG") ? "Config" : "Konfig");

        // Determine TYP and SYFTE based on language
        String typ = testnamn.equalsIgnoreCase("CONFIG") ? "CONFIG" : "KONFIG";
        String syfte = testnamn.equalsIgnoreCase("CONFIG") ? "General Config" : "Generell Konfig";

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"));

        String sql = "INSERT INTO PTLOG (DATUM, TYP, TESTNAMN, SYFTE, ANALYS, PROJEKT, TESTARE) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, typ);
            stmt.setString(3, finalTestnamn);
            stmt.setString(4, syfte);
            stmt.setString(5, beskrivning);
            stmt.setString(6, projekt);
            stmt.setString(7, testare);

            stmt.executeUpdate();
            logger.info("Inserted {} {}: {} for project: {}", syfte, typ, finalTestnamn, projekt);
            return ResponseEntity.ok(syfte + " added successfully with testnamn: " + finalTestnamn);
        } catch (SQLException e) {
            logger.error("Failed to insert {} {}: {}", syfte, typ, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }



    @CrossOrigin(origins = "*")
    @PutMapping("/updateAnalys")
    public ResponseEntity<String> updateAnalys(@RequestBody String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String projekt, testnamn, analys;
        try {
            projekt = getRequiredField(node, "Projekt");
            testnamn = getRequiredField(node, "Testnamn");
            analys = getRequiredField(node, "Analys");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required field: " + e.getMessage());
        }

        String sql = "UPDATE PTLOG SET ANALYS = ? WHERE PROJEKT = ? AND TESTNAMN = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, analys);
            stmt.setString(2, projekt);
            stmt.setString(3, testnamn);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No row found with Projekt: " + projekt + " and Testnamn: " + testnamn);
            }
            logger.info("Updated analysis for test: {} in project: {}", testnamn, projekt);
            return ResponseEntity.ok("Updated " + rows + " row(s)");
        } catch (SQLException e) {
            logger.error("Failed to update analysis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    @CrossOrigin(origins = "*", methods = {RequestMethod.DELETE, RequestMethod.OPTIONS})
    @DeleteMapping("/deleteTest")
    public ResponseEntity<String> deleteTest(@RequestBody String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String projekt, testnamn;
        try {
            projekt = getRequiredField(node, "Projekt");
            testnamn = getRequiredField(node, "Testnamn");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Missing required field: " + e.getMessage());
        }

        String sql = "DELETE FROM PTLOG WHERE PROJEKT = ? AND TESTNAMN = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projekt);
            stmt.setString(2, testnamn);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No test found with Projekt: " + projekt + " and Testnamn: " + testnamn);
            }
            logger.info("Deleted test: {} from project: {}", testnamn, projekt);
            return ResponseEntity.ok("Deleted " + rows + " test(s)");
        } catch (SQLException e) {
            logger.error("Failed to delete test: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    // ✅ Pool monitoring endpoint
    @CrossOrigin(origins = "*")
    @GetMapping("/dbpool")
    public Map<String, Object> dbPoolStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            stats.put("active", hikari.getHikariPoolMXBean().getActiveConnections());
            stats.put("idle", hikari.getHikariPoolMXBean().getIdleConnections());
            stats.put("waiting", hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
            stats.put("total", hikari.getHikariPoolMXBean().getTotalConnections());
            stats.put("maxPoolSize", hikari.getMaximumPoolSize());
        } else {
            stats.put("error", "Not a HikariDataSource");
        }
        return stats;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/dbinfo")
    public ResponseEntity<Map<String, Object>> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Basic database information
            String productName = metaData.getDatabaseProductName();
            String url = metaData.getURL();

            // Determine database type
            String dbType;
            if (productName.toLowerCase().contains("oracle")) {
                dbType = "Oracle";
            } else if (productName.toLowerCase().contains("h2")) {
                dbType = "H2";
            } else {
                dbType = productName;
            }

            // Populate response
            info.put("type", dbType);
            info.put("productName", productName);
            info.put("schema", conn.getSchema());
            info.put("url", maskPassword(url));

            logger.info("Database info retrieved: {}", dbType);
            return ResponseEntity.ok(info);

        } catch (SQLException e) {
            logger.error("Failed to retrieve database info: {}", e.getMessage());
            info.put("error", "Failed to retrieve database information");
            info.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(info);
        }
    }

    // Helper method to mask passwords in connection URLs
    private String maskPassword(String url) {
        if (url == null) return null;
        // Mask password in URLs like: jdbc:oracle:thin:user/password@host:port/service
        return url.replaceAll("(/|:)[^/@:]+(@)", "$1****$2");
    }

    private String getRequiredField(JsonNode node, String fieldName) {
        if (!node.hasNonNull(fieldName)) {
            throw new IllegalArgumentException(fieldName);
        }
        return node.get(fieldName).asText();
    }

    // ANSI colored logger (optional)
    public class ColorLogger {
        private static final Logger LOGGER = LoggerFactory.getLogger("");
        public void logDebug(String logging) { LOGGER.debug("\u001B[92m" + logging + "\u001B[0m"); }
        public void logInfo(String logging) { LOGGER.info("\u001B[93m" + logging + "\u001B[0m"); }
        public void logError(String logging) { LOGGER.error("\u001B[91m" + logging + "\u001B[0m"); }
    }
}