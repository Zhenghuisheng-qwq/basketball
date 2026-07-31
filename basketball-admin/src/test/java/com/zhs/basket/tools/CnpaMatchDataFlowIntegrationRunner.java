package com.zhs.basket.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三轮联调：通过 HTTP + JDBC 验证 submission 主链路。
 * 需 basketball-web(8881) 运行；队长账号需可登录。
 *
 * 用法：-Dintegration.web.base=http://127.0.0.1:8881
 *       -Dintegration.captain.email=...
 *       -Dintegration.captain.password=...
 *       -Dintegration.match.id=900020001
 */
public final class CnpaMatchDataFlowIntegrationRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");
    private static final String WEB_BASE = System.getProperty("integration.web.base", "http://127.0.0.1:8881");
    private static final long MATCH_ID = Long.parseLong(System.getProperty("integration.match.id", "900020001"));
    private static final long MATERIAL_FILE_ID = Long.parseLong(System.getProperty("integration.material.fileId", "2081011198194663426"));

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static final List<String> RESULTS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        resetMatchForTest();
        String token = loginIfConfigured();
        if (token == null) {
            record("HTTP 联调", "SKIP", "未配置 integration.captain.email/password，跳过 HTTP 提交流程");
            printSummary();
            return;
        }
        long submissionId = submitViaHttp(token);
        verifySubmissionInDb(submissionId);
        record("队长提交", "PASS", "submissionId=" + submissionId);
        printSummary();
    }

    private static void resetMatchForTest() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE basket_match SET data_status=0, home_score=0, away_score=0, winner_team_id=NULL WHERE id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_data_submission WHERE match_id=" + MATCH_ID);
            record("测试数据重置", "PASS", "match_id=" + MATCH_ID);
        }
    }

    private static String loginIfConfigured() throws Exception {
        String email = System.getProperty("integration.captain.email");
        String password = System.getProperty("integration.captain.password");
        if (email == null || password == null || email.isBlank()) {
            return null;
        }
        Map<String, String> body = Map.of("email", email, "password", password);
        JsonNode resp = postJson("/basket/member/auth/login/password", body, null);
        if (resp.path("code").asInt() != 200) {
            throw new IllegalStateException("登录失败: " + resp.path("msg").asText());
        }
        return resp.path("data").path("token").asText(null);
    }

    private static long submitViaHttp(String token) throws Exception {
        Map<String, Object> payload = buildSubmitPayload();
        JsonNode resp = postJson("/basket/player/match-data/" + MATCH_ID + "/submit", payload, token);
        if (resp.path("code").asInt() != 200) {
            throw new IllegalStateException("提交失败: " + resp.path("msg").asText());
        }
        return resp.path("data").path("submissionId").asLong();
    }

    private static Map<String, Object> buildSubmitPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("homeScore", 64);
        payload.put("awayScore", 55);
        payload.put("periodScores", List.of(
                period(1, 17, 23), period(2, 17, 8), period(3, 16, 15), period(4, 14, 9)));
        payload.put("teamStats", List.of(
                teamStat("900010001", 64, 20, 19, 6, 3, 0, 0, 26, 46, 5, 15, 7, 9),
                teamStat("900010002", 55, 9, 10, 3, 2, 0, 0, 23, 28, 6, 8, 3, 3)));
        payload.put("playerStats", List.of(
                playerStat("900010001", "2081010979671425000", 18, 5, 6, 2, 0, 7, 12, 2, 5, 2, 2),
                playerStat("900010001", "2081041427005038600", 14, 8, 2, 1, 1, 6, 10, 0, 1, 2, 3),
                playerStat("900010001", "2081072741716336600", 12, 4, 4, 1, 1, 5, 9, 1, 3, 1, 2),
                playerStat("900010001", "2081073127508418600", 10, 2, 3, 1, 1, 4, 8, 2, 4, 0, 0),
                playerStat("900010001", "2081073380093599700", 10, 1, 4, 1, 0, 4, 7, 0, 2, 2, 2),
                playerStat("900010002", "2081019227996807200", 20, 3, 5, 1, 0, 8, 11, 4, 5, 0, 0),
                playerStat("900010002", "2082478335308456000", 18, 4, 2, 1, 1, 7, 9, 2, 3, 2, 2),
                playerStat("900010002", "2082786571525836800", 17, 2, 3, 1, 0, 8, 8, 0, 0, 1, 1)));
        payload.put("materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID)));
        payload.put("remark", "integration-test");
        return payload;
    }

    private static Map<String, Object> period(int no, int home, int away) {
        return Map.of("periodNo", no, "periodType", "REGULAR", "homeScore", home, "awayScore", away);
    }

    private static Map<String, Object> teamStat(String teamId, int pts, int reb, int ast, int stl, int blk, int to, int fouls,
            int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        Map<String, Object> m = new HashMap<>();
        m.put("teamId", teamId);
        m.put("points", pts);
        m.put("rebounds", reb);
        m.put("offensiveRebounds", 0);
        m.put("defensiveRebounds", reb);
        m.put("assists", ast);
        m.put("steals", stl);
        m.put("blocks", blk);
        m.put("turnovers", to);
        m.put("fouls", fouls);
        m.put("fieldGoalsMade", fgm);
        m.put("fieldGoalsAttempted", fga);
        m.put("threePointsMade", tpm);
        m.put("threePointsAttempted", tpa);
        m.put("freeThrowsMade", ftm);
        m.put("freeThrowsAttempted", fta);
        return m;
    }

    private static Map<String, Object> playerStat(String teamId, String playerId, int pts, int reb, int ast, int stl, int blk,
            int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        Map<String, Object> m = new HashMap<>();
        m.put("teamId", teamId);
        m.put("playerId", playerId);
        m.put("points", pts);
        m.put("rebounds", reb);
        m.put("offensiveRebounds", 0);
        m.put("defensiveRebounds", reb);
        m.put("assists", ast);
        m.put("steals", stl);
        m.put("blocks", blk);
        m.put("turnovers", 0);
        m.put("fouls", 0);
        m.put("fieldGoalsMade", fgm);
        m.put("fieldGoalsAttempted", fga);
        m.put("threePointsMade", tpm);
        m.put("threePointsAttempted", tpa);
        m.put("freeThrowsMade", ftm);
        m.put("freeThrowsAttempted", fta);
        m.put("plusMinus", 0);
        return m;
    }

    private static JsonNode postJson(String path, Object body, String token) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(WEB_BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(response.body());
    }

    private static void verifySubmissionInDb(long submissionId) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT submission_status, audit_status FROM basket_match_data_submission WHERE id=" + submissionId);
            if (!rs.next()) {
                throw new IllegalStateException("submission 不存在");
            }
            if (rs.getInt("submission_status") != 1) {
                throw new IllegalStateException("submission_status 应为 1");
            }
            ResultSet audit = stmt.executeQuery("SELECT audit_action FROM basket_match_data_audit WHERE submission_id=" + submissionId + " AND audit_action='SUBMIT'");
            if (!audit.next()) {
                throw new IllegalStateException("audit SUBMIT 未写入");
            }
            ResultSet match = stmt.executeQuery("SELECT data_status FROM basket_match WHERE id=" + MATCH_ID);
            match.next();
            if (match.getInt("data_status") != 1) {
                throw new IllegalStateException("match.data_status 应为 SUBMITTED(1)");
            }
        }
    }

    private static void record(String name, String status, String detail) {
        RESULTS.add(name + " | " + status + " | " + detail);
        System.out.println("[integration] " + name + ": " + status + " - " + detail);
    }

    private static void printSummary() {
        System.out.println("===== Integration Summary =====");
        RESULTS.forEach(System.out::println);
    }
}
