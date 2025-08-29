package se.ptlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;
import co.elastic.apm.attach.ElasticApmAttacher;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OpenAPIDefinition(
        info = @Info(
                title = "PT-Log",
                version = "v1",
                description = "The PT-Log API provides endpoints to manage and track tests for various projects.\n\n" +
                        "Endpoints:\n" +
                        "1. **GET /healthcheck** - Checks if the API is running.\n" +
                        "2. **GET /getData?projekt={projekt}** - Retrieves all test logs for the specified project.\n" +
                        "3. **GET /populate** - Returns a list of all unique project names.\n" +
                        "4. **POST /createProject** - Creates a new project. Requires JSON body: {\"Projekt\": \"ProjectName\"}.\n" +
                        "5. **POST /insert** - Inserts a new test log. Requires JSON body with fields: Datum (ISO 8601), Typ, Testnamn, Syfte, Projekt, Testare.\n" +
                        "6. **PUT /updateAnalys** - Updates the ANALYS column for a given project and test name. Requires JSON body: {\"Projekt\": \"...\", \"Testnamn\": \"...\", \"Analys\": \"...\"}.\n\n" +
                        "Note: This API connects to an Oracle database. Ensure proper credentials and network access.\n" +
                        "Data persistence is real (stored in Oracle), but test logs and projects are managed via this API."
        )
)
@SpringBootApplication(scanBasePackages = "se.ptlog")
@RestController
@EnableScheduling
public class PtLog {

    private static final Logger logger = LoggerFactory.getLogger(PtLog.class);

    public static void main(String[] args) {
        SpringApplication.run(PtLog.class, args);
        ElasticApmAttacher.attach();
    }

    // 🔹 Fetch DB connection info from application.properties
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @CrossOrigin(origins = "*")
    @GetMapping(value = "healthcheck")
    public ResponseEntity<String> healthcheck() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        headers.add(HttpHeaders.CONTENT_ENCODING, "UTF-8");
        return ResponseEntity.ok()
                .headers(headers)
                .body("{\"status\":\"ok\",\"service\":\"API Health Check\"}");
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/getData")
    public List<Map<String, Object>> getData(@RequestParam String projekt) throws SQLException {

        String sql = "SELECT " +
                "TO_CHAR(DATUM, 'YYYY-MM-DD HH24:MI') AS DATUM, " +
                "TYP, " +
                "TESTNAMN, " +
                "SYFTE, " +
                "ANALYS, " +
                "PROJEKT, " +
                "TESTARE " +
                "FROM ptlog " +
                "WHERE PROJEKT = '" + projekt + "' " +
                "ORDER BY DATUM";

        return OraSQL(sql);
    }

    public List<Map<String, Object>> OraSQL(String query) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            ResultSetMetaData rsmd = rs.getMetaData();
            int colCount = rsmd.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(rsmd.getColumnName(i), rs.getObject(i));
                }
                resultList.add(row);
            }
        }
        return resultList;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/populate")
    public List<String> getAllProjekts() throws SQLException {
        String sql = "SELECT DISTINCT NAMN FROM PTLOG_PROJEKT ORDER BY NAMN";
        List<String> projekts = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                projekts.add(rs.getString("NAMN"));
            }
        }

        return projekts;
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

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projektName.trim());
            int rows = stmt.executeUpdate();
            return ResponseEntity.ok("Inserted project: " + projektName + " (" + rows + " row(s))");

        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    public int countRowsForProject(String projekt) throws SQLException {
        String sql = "SELECT COUNT(*) AS CNT FROM ptlog WHERE PROJEKT = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projekt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("CNT");
                }
            }
        }
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
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
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
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required field: " + e.getMessage());
        }

        int count = 0;
        try {
            count = countRowsForProject(projekt);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to count rows: " + e.getMessage());
        }

        String counterStr = String.format("%02d", count + 1);

        switch (typ) {
            case "Referenstest":   testnamn = "REF_" + testnamn; break;
            case "Belastningstest": testnamn = "BEL_" + testnamn; break;
            case "Utmattningstest": testnamn = "UTM_" + testnamn; break;
            case "Maxtest":        testnamn = "MAX_" + testnamn; break;
            case "Skapa":          testnamn = "SKA_" + testnamn; break;
            case "Verifikationstest":          testnamn = "VER_" + testnamn; break;
        }

        testnamn = counterStr + "_" + testnamn;

        String sql = "INSERT INTO PTLOG (DATUM, TYP, TESTNAMN, SYFTE, PROJEKT, TESTARE) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(Instant.parse(datum)));
            stmt.setString(2, typ);
            stmt.setString(3, testnamn);
            stmt.setString(4, syfte);
            stmt.setString(5, projekt);
            stmt.setString(6, testare);

            int rows = stmt.executeUpdate();
            return ResponseEntity.ok("Inserted " + rows + " row(s) with testnamn: " + testnamn);

        } catch (SQLException e) {
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
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON: " + e.getOriginalMessage());
        }

        String projekt, testnamn, analys;
        try {
            projekt = getRequiredField(node, "Projekt");
            testnamn = getRequiredField(node, "Testnamn");
            analys = getRequiredField(node, "Analys");
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing required field: " + e.getMessage());
        }

        String sql = "UPDATE PTLOG SET ANALYS = ? WHERE PROJEKT = ? AND TESTNAMN = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, analys);
            stmt.setString(2, projekt);
            stmt.setString(3, testnamn);

            int rows = stmt.executeUpdate();

            if (rows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No row found with Projekt: " + projekt + " and Testnamn: " + testnamn);
            }

            return ResponseEntity.ok("Updated " + rows + " row(s) for Projekt: " + projekt + ", Testnamn: " + testnamn);

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        }
    }

    private String getRequiredField(JsonNode node, String fieldName) {
        if (!node.hasNonNull(fieldName)) {
            throw new IllegalArgumentException(fieldName);
        }
        return node.get(fieldName).asText();
    }

    public class ColorLogger {
        private static final Logger LOGGER = LoggerFactory.getLogger("");

        public void logDebug(String logging) { LOGGER.debug("\u001B[92m" + logging + "\u001B[0m"); }
        public void logInfo(String logging) { LOGGER.info("\u001B[93m" + logging + "\u001B[0m"); }
        public void logError(String logging) { LOGGER.error("\u001B[91m" + logging + "\u001B[0m"); }
    }
}
