package com.zhs.basket.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
 * CNPA P0 全链路联调：提交 → 驳回 → 重提 → 通过 → 正式表校验。
 * 需 basketball-web:8881、basketball-admin:8080 已启动。
 */
public final class CnpaMatchDataP0FullIntegrationRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");
    private static final String WEB_BASE = System.getProperty("integration.web.base", "http://127.0.0.1:8881");
    private static final String ADMIN_BASE = System.getProperty("integration.admin.base", "http://127.0.0.1:8080");
    private static final long MATCH_ID = Long.parseLong(System.getProperty("integration.match.id", "900020001"));
    private static final long MATERIAL_FILE_ID = Long.parseLong(System.getProperty("integration.material.fileId", "2081011198194663426"));
    private static final String CAPTAIN_EMAIL = System.getProperty("integration.captain.email", "111@qq.com");
    private static final String CAPTAIN_PASSWORD = System.getProperty("integration.captain.password", "Test1234");
    private static final String ADMIN_USER = System.getProperty("integration.admin.user", "admin");
    private static final String ADMIN_PASSWORD = System.getProperty("integration.admin.password", "admin123");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final List<String> RESULTS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ensureCaptainPassword();
        resetMatchForTest();
        String captainToken = webLogin();
        record("Web 登录", "PASS", CAPTAIN_EMAIL);

        long submissionId = submitViaHttp(captainToken);
        verifySubmissionInDb(submissionId, 1, 1);
        record("队长提交", "PASS", "submissionId=" + submissionId);

        String adminToken = adminLogin();
        record("Admin 登录", "PASS", ADMIN_USER);

        rejectSubmission(adminToken, submissionId, "联调驳回：材料不清晰");
        verifySubmissionInDb(submissionId, 3, 4);
        record("审核驳回", "PASS", "submissionId=" + submissionId);

        long resubmitId = resubmitViaHttp(captainToken);
        if (resubmitId == submissionId) {
            throw new IllegalStateException("重提应生成新 submission");
        }
        verifySubmissionInDb(resubmitId, 1, 1);
        record("驳回重提", "PASS", "newSubmissionId=" + resubmitId + ", version=2");

        approveSubmission(adminToken, resubmitId);
        verifySubmissionInDb(resubmitId, 2, 2);
        verifyMatchDataStatus(2);
        verifyOfficialData();
        record("审核通过+正式同步", "PASS", "match score 64:55");

        printSummary();
    }

    private static void ensureCaptainPassword() throws Exception {
        String hash = new BCryptPasswordEncoder().encode(CAPTAIN_PASSWORD);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE basket_member_account SET password_hash='" + hash
                    + "', update_time=NOW() WHERE email='" + CAPTAIN_EMAIL + "'");
        }
    }

    private static void resetMatchForTest() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM basket_stat_sync_record WHERE match_id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_official_result WHERE match_id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_player_stat WHERE match_id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_team_stat WHERE match_id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_period_score WHERE match_id=" + MATCH_ID);
            stmt.execute("UPDATE basket_match_team SET score=0, result_type='LOSS', is_winner=0 WHERE match_id=" + MATCH_ID);
            stmt.execute("UPDATE basket_match SET data_status=0, home_score=0, away_score=0, winner_team_id=NULL WHERE id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_data_audit WHERE match_id=" + MATCH_ID);
            stmt.execute("DELETE FROM basket_match_data_submission WHERE match_id=" + MATCH_ID);
        }
        record("测试数据重置", "PASS", "match_id=" + MATCH_ID);
    }

    private static String webLogin() throws Exception {
        JsonNode resp = postJson(WEB_BASE, "/basket/member/auth/login/password",
                Map.of("email", CAPTAIN_EMAIL, "password", CAPTAIN_PASSWORD), null);
        assertOk(resp, "Web 登录失败");
        return resp.path("data").path("token").asText();
    }

    private static String adminLogin() throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/login",
                Map.of("username", ADMIN_USER, "password", ADMIN_PASSWORD), null);
        assertOk(resp, "Admin 登录失败");
        return resp.path("token").asText();
    }

    private static long submitViaHttp(String token) throws Exception {
        JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                buildSubmitPayload(), token);
        assertOk(resp, "提交失败");
        return resp.path("data").path("submissionId").asLong();
    }

    private static long resubmitViaHttp(String token) throws Exception {
        Map<String, Object> payload = buildSubmitPayload();
        payload.put("homeScore", 64);
        payload.put("awayScore", 55);
        payload.put("remark", "integration-resubmit");
        JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/resubmit",
                payload, token);
        assertOk(resp, "重提失败");
        return resp.path("data").path("submissionId").asLong();
    }

    private static void rejectSubmission(String adminToken, long submissionId, String reason) throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/basket/matchAudit/submission/" + submissionId + "/reject",
                Map.of("rejectReason", reason), adminToken);
        assertOk(resp, "驳回失败");
    }

    private static void approveSubmission(String adminToken, long submissionId) throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/basket/matchAudit/submission/" + submissionId + "/approve",
                Map.of("auditRemark", "联调通过"), adminToken);
        assertOk(resp, "审核通过失败");
    }

    private static Map<String, Object> buildSubmitPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("homeScore", 64);
        payload.put("awayScore", 55);
        payload.put("periodScores", List.of(
                period(1, 17, 23), period(2, 17, 8), period(3, 16, 15), period(4, 14, 9)));
        payload.put("teamStats", List.of(
                teamStat("900010001", 64, 20, 19, 6, 3, 7, 8, 26, 46, 5, 15, 7, 9),
                teamStat("900010002", 55, 12, 15, 5, 2, 9, 7, 21, 41, 4, 13, 9, 10)));
        payload.put("playerStats", List.of(
                playerStat("900010001", "2081010979671425000", 18, 5, 6, 2, 0, 7, 12, 2, 5, 2, 2),
                playerStat("900010001", "2081041427005038600", 14, 8, 2, 1, 1, 6, 10, 0, 1, 2, 3),
                playerStat("900010001", "2081072741716336600", 12, 4, 4, 1, 1, 5, 9, 1, 3, 1, 2),
                playerStat("900010001", "2081073127508418600", 10, 2, 3, 1, 1, 4, 8, 2, 4, 0, 0),
                playerStat("900010001", "2081073380093599700", 10, 1, 4, 1, 0, 4, 7, 0, 2, 2, 2),
                playerStat("900010002", "2081019227996807200", 20, 3, 5, 1, 0, 6, 11, 2, 5, 2, 2),
                playerStat("900010002", "2082478335308456000", 18, 4, 2, 1, 1, 5, 9, 1, 3, 1, 2),
                playerStat("900010002", "2082786571525836800", 17, 2, 3, 1, 0, 3, 6, 0, 1, 2, 2)));
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

    private static JsonNode postJson(String base, String path, Object body, String token) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(response.body());
    }

    private static void assertOk(JsonNode resp, String prefix) {
        int code = resp.path("code").asInt(-1);
        if (code != 200) {
            throw new IllegalStateException(prefix + ": " + resp.path("msg").asText("unknown"));
        }
    }

    private static void verifySubmissionInDb(long submissionId, int submissionStatus, int matchDataStatus) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT submission_status FROM basket_match_data_submission WHERE id=" + submissionId);
            if (!rs.next() || rs.getInt(1) != submissionStatus) {
                throw new IllegalStateException("submission_status 期望 " + submissionStatus);
            }
            ResultSet periods = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_match_data_submission_period WHERE submission_id=" + submissionId);
            periods.next();
            if (periods.getInt(1) != 4) {
                throw new IllegalStateException("period 子表应为 4 条");
            }
            ResultSet materials = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_match_data_submission_material WHERE submission_id=" + submissionId);
            materials.next();
            if (materials.getInt(1) < 1) {
                throw new IllegalStateException("material 子表缺失");
            }
            verifyMatchDataStatus(matchDataStatus);
        }
    }

    private static void verifyMatchDataStatus(int expected) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet match = stmt.executeQuery("SELECT data_status FROM basket_match WHERE id=" + MATCH_ID);
            match.next();
            if (match.getInt(1) != expected) {
                throw new IllegalStateException("match.data_status 期望 " + expected);
            }
        }
    }

    private static void verifyOfficialData() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet match = stmt.executeQuery(
                    "SELECT home_score, away_score, data_status, winner_team_id FROM basket_match WHERE id=" + MATCH_ID);
            if (!match.next()) {
                throw new IllegalStateException("比赛不存在");
            }
            if (match.getInt("home_score") != 64 || match.getInt("away_score") != 55) {
                throw new IllegalStateException("比分未同步");
            }
            if (match.getInt("data_status") != 2) {
                throw new IllegalStateException("data_status 应为 CONFIRMED(2)");
            }
            if (match.getLong("winner_team_id") != 900010001L) {
                throw new IllegalStateException("winner_team_id 错误");
            }
            ResultSet period = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_match_period_score WHERE match_id=" + MATCH_ID);
            period.next();
            if (period.getInt(1) < 4) {
                throw new IllegalStateException("正式分节比分缺失");
            }
            ResultSet teamStat = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_match_team_stat WHERE match_id=" + MATCH_ID);
            teamStat.next();
            if (teamStat.getInt(1) < 2) {
                throw new IllegalStateException("正式球队 stat 缺失");
            }
            ResultSet playerStat = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_match_player_stat WHERE match_id=" + MATCH_ID);
            playerStat.next();
            if (playerStat.getInt(1) < 8) {
                throw new IllegalStateException("正式球员 stat 缺失");
            }
            ResultSet sync = stmt.executeQuery(
                    "SELECT COUNT(*) FROM basket_stat_sync_record WHERE match_id=" + MATCH_ID);
            sync.next();
            if (sync.getInt(1) < 1) {
                throw new IllegalStateException("stat_sync_record 缺失");
            }
        }
    }

    private static void record(String name, String status, String detail) {
        RESULTS.add(name + " | " + status + " | " + detail);
        System.out.println("[integration] " + name + ": " + status + " - " + detail);
    }

    private static void printSummary() {
        System.out.println("===== P0 Full Integration Summary =====");
        RESULTS.forEach(System.out::println);
    }
}
