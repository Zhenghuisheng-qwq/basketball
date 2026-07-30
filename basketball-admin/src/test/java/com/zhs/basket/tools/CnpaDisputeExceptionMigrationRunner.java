package com.zhs.basket.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 执行 CNPA 争议/异常 P1 migration 与权限补丁。
 */
public final class CnpaDisputeExceptionMigrationRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");

    private CnpaDisputeExceptionMigrationRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath();
        if (projectRoot.getFileName().toString().equals("basketball-admin")) {
            projectRoot = projectRoot.getParent();
        }
        Path migration = projectRoot.resolve("docs/sql/cnpa-match-data-dispute-exception-p1-migration.sql");
        Path permissions = projectRoot.resolve("docs/sql/cnpa-match-data-dispute-exception-p1-permissions.sql");
        if (!Files.exists(migration)) {
            throw new IllegalStateException("Migration file not found: " + migration);
        }
        if (!Files.exists(permissions)) {
            throw new IllegalStateException("Permissions file not found: " + permissions);
        }

        System.out.println("[migration] Running: " + migration);
        runScript(Files.readString(migration, StandardCharsets.UTF_8));
        System.out.println("[migration] First pass OK, running second pass (idempotency)...");
        runScript(Files.readString(migration, StandardCharsets.UTF_8));
        System.out.println("[migration] Second pass OK");

        System.out.println("[migration] Running permissions: " + permissions);
        runScript(Files.readString(permissions, StandardCharsets.UTF_8));
        System.out.println("[migration] Permissions OK");

        verify();
    }

    private static void runScript(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void verify() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            String[] tables = {
                    "basket_match_data_dispute_material",
                    "basket_match_exception_application",
                    "basket_match_exception_material",
                    "basket_match_exception_audit"
            };
            for (String table : tables) {
                assertTableExists(stmt, table);
            }
            assertColumnExists(stmt, "basket_match_data_dispute", "active_dispute_slot");
            assertColumnExists(stmt, "basket_match_data_dispute", "dispute_deadline_time");
            assertIndexExists(stmt, "basket_match_data_dispute", "uk_match_active_dispute");
            assertIndexExists(stmt, "basket_match_exception_application", "uk_match_active_exception");

            ResultSet menuRs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM sys_menu WHERE menu_id IN (2240,2241,2242,2243,2244,2250,2251,2252,2253)");
            menuRs.next();
            if (menuRs.getInt(1) < 9) {
                throw new IllegalStateException("Dispute/exception menu permissions incomplete");
            }
            System.out.println("[verify] menu permissions exist (9 entries)");
        }
    }

    private static void assertTableExists(Statement stmt, String table) throws SQLException {
        ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '"
                        + table + "'");
        rs.next();
        if (rs.getInt(1) != 1) {
            throw new IllegalStateException("Missing table: " + table);
        }
        System.out.println("[verify] table exists: " + table);
    }

    private static void assertColumnExists(Statement stmt, String table, String column) throws SQLException {
        ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                        + " AND table_name = '" + table + "' AND column_name = '" + column + "'");
        rs.next();
        if (rs.getInt(1) != 1) {
            throw new IllegalStateException("Missing column: " + table + "." + column);
        }
        System.out.println("[verify] column exists: " + table + "." + column);
    }

    private static void assertIndexExists(Statement stmt, String table, String indexName) throws SQLException {
        ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                        + " AND table_name = '" + table + "' AND index_name = '" + indexName + "'");
        rs.next();
        if (rs.getInt(1) < 1) {
            throw new IllegalStateException("Missing index: " + table + "." + indexName);
        }
        System.out.println("[verify] index exists: " + table + "." + indexName);
    }
}
