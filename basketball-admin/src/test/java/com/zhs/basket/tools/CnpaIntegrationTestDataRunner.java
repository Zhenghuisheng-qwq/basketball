package com.zhs.basket.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 执行联调测试数据 SQL。
 */
public final class CnpaIntegrationTestDataRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.49:3306/basketball?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");

    public static void main(String[] args) throws Exception {
        Path projectRoot = Path.of("").toAbsolutePath();
        if (projectRoot.getFileName().toString().equals("basketball-admin")) {
            projectRoot = projectRoot.getParent();
        }
        Path script = projectRoot.resolve("docs/sql/cnpa-match-data-integration-test-data.sql");
        if (!Files.exists(script)) {
            throw new IllegalStateException("Test data file not found: " + script);
        }
        String sql = Files.readString(script, StandardCharsets.UTF_8);
        System.out.println("[test-data] Running: " + script);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        System.out.println("[test-data] OK");
    }
}
