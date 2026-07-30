package com.zhs.basket.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * 本地/联调环境执行 submission P0 migration（MCP 禁止 DDL 时使用）。
 * 用法：mvn -f basketball-admin/pom.xml test-compile exec:java
 *   -Dexec.classpathScope=test
 *   -Dexec.mainClass=com.zhs.basket.tools.CnpaSubmissionMigrationRunner
 */
public final class CnpaSubmissionMigrationRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");

    private CnpaSubmissionMigrationRunner() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath();
        if (projectRoot.getFileName().toString().equals("basketball-admin")) {
            projectRoot = projectRoot.getParent();
        }
        Path migration = projectRoot.resolve("docs/sql/cnpa-match-data-submission-p0-migration.sql");
        if (!Files.exists(migration)) {
            throw new IllegalStateException("Migration file not found: " + migration);
        }
        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        System.out.println("[migration] Running: " + migration);
        runScript(sql);
        System.out.println("[migration] First pass OK, running second pass (idempotency)...");
        runScript(sql);
        System.out.println("[migration] Second pass OK");
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
                    "basket_match_data_submission",
                    "basket_match_data_submission_period",
                    "basket_match_data_submission_team_stat",
                    "basket_match_data_submission_player_stat",
                    "basket_match_data_submission_material"
            };
            for (String table : tables) {
                var rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '"
                                + table + "'");
                rs.next();
                if (rs.getInt(1) != 1) {
                    throw new IllegalStateException("Missing table: " + table);
                }
                System.out.println("[verify] table exists: " + table);
            }
            String[] auditCols = {"submission_id", "audit_action", "before_status", "after_status", "auditor_name"};
            for (String col : auditCols) {
                var rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                                + " AND table_name = 'basket_match_data_audit' AND column_name = '" + col + "'");
                rs.next();
                if (rs.getInt(1) != 1) {
                    throw new IllegalStateException("Missing audit column: " + col);
                }
                System.out.println("[verify] audit column exists: " + col);
            }
            var slotRs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                            + " AND table_name = 'basket_match_data_submission' AND column_name = 'active_submission_slot'");
            slotRs.next();
            if (slotRs.getInt(1) != 1) {
                throw new IllegalStateException("Missing active_submission_slot column");
            }
            System.out.println("[verify] active_submission_slot exists");
            var idxRs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                            + " AND table_name = 'basket_match_data_submission' AND index_name = 'uk_match_active_submission'");
            idxRs.next();
            if (idxRs.getInt(1) < 1) {
                throw new IllegalStateException("Missing uk_match_active_submission index");
            }
            System.out.println("[verify] uk_match_active_submission exists");
        }
    }
}
