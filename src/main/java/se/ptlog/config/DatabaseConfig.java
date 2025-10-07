package se.ptlog.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${db.type:oracle}")
    private String dbType;

    // Oracle properties
    @Value("${spring.datasource.url:}")
    private String oracleUrl;

    @Value("${spring.datasource.username:}")
    private String oracleUsername;

    @Value("${spring.datasource.password:}")
    private String oraclePassword;

    @Value("${oracle.auto.init:false}")
    private boolean oracleAutoInit;

    // H2 properties
    @Value("${h2.file.path:./data/ptlog}")
    private String h2FilePath;

    @Value("${h2.auto.init:true}")
    private boolean h2AutoInit;

    @Value("${h2.username:sa}")
    private String h2Username;

    @Value("${h2.password:}")
    private String h2Password;

    // HikariCP properties
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        if ("h2".equalsIgnoreCase(dbType)) {
            configureH2(config);
        } else {
            configureOracle(config);
        }

        // Common HikariCP settings
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource dataSource = new HikariDataSource(config);

        // Initialize schema if needed
        if ("h2".equalsIgnoreCase(dbType) && h2AutoInit) {
            initializeH2Schema(dataSource);
        } else if ("oracle".equalsIgnoreCase(dbType) && oracleAutoInit) {
            initializeOracleSchema(dataSource);
        }

        return dataSource;
    }

    private void configureH2(HikariConfig config) {
        String url = "jdbc:h2:file:" + h2FilePath + ";MODE=Oracle;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

        config.setJdbcUrl(url);
        config.setUsername(h2Username);
        config.setPassword(h2Password);
        config.setDriverClassName("org.h2.Driver");

        logger.info("✅ Configured H2 file database at: {}", h2FilePath);
    }

    private void configureOracle(HikariConfig config) {
        config.setJdbcUrl(oracleUrl);
        config.setUsername(oracleUsername);
        config.setPassword(oraclePassword);
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        logger.info("✅ Configured Oracle database");
    }

    private void initializeH2Schema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (schemaExists(conn, "H2")) {
                logger.info("H2 schema already exists, skipping initialization");
                return;
            }

            logger.info("Initializing H2 database schema...");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema-h2.sql"));
            populator.execute(dataSource);

            logger.info("✅ H2 schema initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize H2 schema: {}", e.getMessage(), e);
        }
    }

    private void initializeOracleSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (schemaExists(conn, "Oracle")) {
                logger.info("Oracle schema already exists, skipping initialization");
                return;
            }

            logger.info("Initializing Oracle database schema...");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema-oracle.sql"));
            populator.execute(dataSource);

            logger.info("✅ Oracle schema initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Oracle schema: {}", e.getMessage(), e);
        }
    }

    private boolean schemaExists(Connection conn, String dbType) {
        try (Statement stmt = conn.createStatement()) {
            // Check if PTLOG_PROJEKT table exists
            if ("H2".equals(dbType)) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                                "WHERE TABLE_NAME = 'PTLOG_PROJEKT'"
                );
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            } else {
                // Oracle
                ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'PTLOG_PROJEKT'"
                );
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking if schema exists: {}", e.getMessage());
        }
        return false;
    }
}