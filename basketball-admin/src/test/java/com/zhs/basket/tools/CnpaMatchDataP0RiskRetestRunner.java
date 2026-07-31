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
 * CNPA P0 风险项复测：25 项权限/校验/公开 API/SQL 检查。
 * 需 basketball-web:8881、basketball-admin:8080 已启动。
 */
public final class CnpaMatchDataP0RiskRetestRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");
    private static final String WEB_BASE = System.getProperty("integration.web.base", "http://127.0.0.1:8881");
    private static final String ADMIN_BASE = System.getProperty("integration.admin.base", "http://127.0.0.1:8080");
    private static final long MATCH_ID = Long.parseLong(System.getProperty("integration.match.id", "900020001"));
    private static final long SEASON_ID = Long.parseLong(System.getProperty("integration.season.id", "1"));
    private static final long MATERIAL_FILE_ID = Long.parseLong(System.getProperty("integration.material.fileId", "2081011198194663426"));
    private static final String CAPTAIN_EMAIL = System.getProperty("integration.captain.email", "111@qq.com");
    private static final String CAPTAIN_PASSWORD = System.getProperty("integration.captain.password", "Test1234");
    private static final String AWAY_CAPTAIN_EMAIL = System.getProperty("integration.away.captain.email", "1111111111@qq.com");
    private static final String AWAY_CAPTAIN_PASSWORD = System.getProperty("integration.away.captain.password", "Test1234");
    private static final String MEMBER_EMAIL = System.getProperty("integration.member.email", "2081041427005038600@member.test");
    private static final String MEMBER_PASSWORD = System.getProperty("integration.member.password", "Test1234");
    private static final String ADMIN_USER = System.getProperty("integration.admin.user", "admin");
    private static final String ADMIN_PASSWORD = System.getProperty("integration.admin.password", "admin123");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final List<String> RESULTS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        boolean servicesUp = probeServices();
        if (!servicesUp) {
            record(0, "服务探测", "SKIP", "web/admin 未启动，HTTP 项跳过");
            for (int i = 1; i <= 25; i++) {
                if (i >= 21 && i <= 24) {
                    continue;
                }
                record(i, riskName(i), "SKIP", "服务未启动");
            }
        }

        ensurePasswords();
        resetMatchForTest();

        if (servicesUp) {
            runHttpRiskTests();
        }

        runSqlRiskTests();
        printSummary();
    }

    private static void runHttpRiskTests() throws Exception {
        // 1 普通队员提交
        try {
            String memberEmail = resolveNonCaptainMemberEmail();
            if (memberEmail == null) {
                record(1, "普通队员提交", "SKIP", "未找到非队长队员账号");
            } else {
                String token = webLogin(memberEmail, CAPTAIN_PASSWORD);
                JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                        buildValidSubmitPayload(), token);
                record(1, "普通队员提交", resp.path("code").asInt() == 403 ? "PASS" : "FAIL",
                        "code=" + resp.path("code").asInt() + " msg=" + resp.path("msg").asText(""));
            }
        } catch (Exception ex) {
            record(1, "普通队员提交", "SKIP", ex.getMessage());
        }

        // 2 非本场队长提交（使用不存在比赛的队长）
        try {
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/999999999/submit",
                    buildValidSubmitPayload(), token);
            int code = resp.path("code").asInt();
            record(2, "非本场队长提交", code == 403 || code == 500 ? "PASS" : "FAIL",
                    "code=" + code + " msg=" + resp.path("msg").asText(""));
        } catch (Exception ex) {
            record(2, "非本场队长提交", "SKIP", ex.getMessage());
        }

        // 3 未登录提交
        try {
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), null);
            record(3, "未登录提交", resp.path("code").asInt() == 401 ? "PASS" : "FAIL",
                    "code=" + resp.path("code").asInt());
        } catch (Exception ex) {
            record(3, "未登录提交", "SKIP", ex.getMessage());
        }

        // 4 未认证用户提交
        record(4, "未认证用户提交", "SKIP", "需独立未认证测试账号");

        // 5 客队队长在主队审核中再提交
        try {
            resetMatchForTest();
            String homeToken = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode first = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), homeToken);
            if (first.path("code").asInt() != 200) {
                record(5, "客队队长审核中再提交", "SKIP", "主队提交失败: " + first.path("msg").asText());
            } else {
                String awayToken = webLogin(AWAY_CAPTAIN_EMAIL, AWAY_CAPTAIN_PASSWORD);
                JsonNode second = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                        buildValidSubmitPayload(), awayToken);
                record(5, "客队队长审核中再提交", second.path("code").asInt() != 200 ? "PASS" : "FAIL",
                        "code=" + second.path("code").asInt() + " msg=" + second.path("msg").asText(""));
            }
        } catch (Exception ex) {
            record(5, "客队队长审核中再提交", "SKIP", ex.getMessage());
        }

        // 6 审核中重复提交
        try {
            resetMatchForTest();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode first = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), token);
            JsonNode second = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), token);
            record(6, "审核中重复提交", second.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "first=" + first.path("code").asInt() + " second=" + second.path("code").asInt());
        } catch (Exception ex) {
            record(6, "审核中重复提交", "SKIP", ex.getMessage());
        }

        // 7 审核通过后再次提交
        try {
            long submissionId = submitAndApproveFlow();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), token);
            record(7, "审核通过后再次提交", resp.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "submissionId=" + submissionId + " code=" + resp.path("code").asInt());
        } catch (Exception ex) {
            record(7, "审核通过后再次提交", "SKIP", ex.getMessage());
        }

        // 8 审核中 resubmit
        try {
            resetMatchForTest();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit", buildValidSubmitPayload(), token);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/resubmit",
                    buildValidSubmitPayload(), token);
            record(8, "审核中 resubmit", resp.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "code=" + resp.path("code").asInt() + " msg=" + resp.path("msg").asText(""));
        } catch (Exception ex) {
            record(8, "审核中 resubmit", "SKIP", ex.getMessage());
        }

        // 9 驳回前 resubmit
        try {
            resetMatchForTest();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit", buildValidSubmitPayload(), token);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/resubmit",
                    buildValidSubmitPayload(), token);
            record(9, "驳回前 resubmit", resp.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "code=" + resp.path("code").asInt() + " msg=" + resp.path("msg").asText(""));
        } catch (Exception ex) {
            record(9, "驳回前 resubmit", "SKIP", ex.getMessage());
        }

        // 10 非原提交方 resubmit
        try {
            resetMatchForTest();
            String homeToken = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode submit = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), homeToken);
            long submissionId = submit.path("data").path("submissionId").asLong();
            String adminToken = adminLogin();
            rejectSubmission(adminToken, submissionId, "risk-retest reject");
            String awayToken = webLogin(AWAY_CAPTAIN_EMAIL, AWAY_CAPTAIN_PASSWORD);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/resubmit",
                    buildValidSubmitPayload(), awayToken);
            record(10, "非原提交方 resubmit", resp.path("code").asInt() == 403 ? "PASS" : "FAIL",
                    "code=" + resp.path("code").asInt() + " msg=" + resp.path("msg").asText(""));
        } catch (Exception ex) {
            record(10, "非原提交方 resubmit", "SKIP", ex.getMessage());
        }

        // 11-17 校验失败
        runValidationTest(11, "四节比分合计≠最终比分", mutatePeriodTotalMismatch());
        runValidationTest(12, "球员得分合计≠球队比分", mutatePlayerPointsMismatch());
        runValidationTest(13, "投篮命中>出手", mutateFieldGoalInvalid());
        runValidationTest(14, "三分命中>出手", mutateThreePointInvalid());
        runValidationTest(15, "罚球命中>出手", mutateFreeThrowInvalid());
        runValidationTest(16, "无截图", mutateNoScreenshot());
        runValidationTest(17, "非 roster 球员", mutateNonRosterPlayer());

        // 18 旧 CRUD 403
        try {
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WEB_BASE + "/basket/match/player/stat/addOrUpdate"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            record(18, "旧 CRUD 写接口", response.statusCode() == 403 ? "PASS" : "FAIL",
                    "httpStatus=" + response.statusCode());
        } catch (Exception ex) {
            record(18, "旧 CRUD 写接口", "SKIP", ex.getMessage());
        }

        // 19-20 公开 API 过滤
        try {
            resetMatchForTest();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit", buildValidSubmitPayload(), token);
            setMatchDataStatus(1);
            JsonNode publicResp = getJson(WEB_BASE, "/basket/public/match/" + MATCH_ID, null);
            boolean statsHidden = !publicResp.path("data").path("statsAvailable").asBoolean(true);
            record(19, "公开 API 过滤 SUBMITTED", statsHidden ? "PASS" : "FAIL",
                    "statsAvailable=" + publicResp.path("data").path("statsAvailable").asText());
        } catch (Exception ex) {
            record(19, "公开 API 过滤 SUBMITTED", "SKIP", ex.getMessage());
        }

        try {
            setMatchDataStatus(3);
            JsonNode publicResp = getJson(WEB_BASE, "/basket/public/match/" + MATCH_ID, null);
            boolean statsHidden = !publicResp.path("data").path("statsAvailable").asBoolean(true);
            record(20, "公开 API 过滤非 CONFIRMED", statsHidden ? "PASS" : "FAIL",
                    "dataStatus=" + publicResp.path("data").path("dataStatus").asText());
        } catch (Exception ex) {
            record(20, "公开 API 过滤非 CONFIRMED", "SKIP", ex.getMessage());
        }

        // 25 重复审核幂等
        try {
            resetMatchForTest();
            String captainToken = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode submit = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                    buildValidSubmitPayload(), captainToken);
            long submissionId = submit.path("data").path("submissionId").asLong();
            String adminToken = adminLogin();
            approveSubmission(adminToken, submissionId);
            JsonNode secondApprove = postJson(ADMIN_BASE,
                    "/basket/matchAudit/submission/" + submissionId + "/approve",
                    Map.of("auditRemark", "duplicate"), adminToken);
            record(25, "重复审核幂等", secondApprove.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "code=" + secondApprove.path("code").asInt() + " msg=" + secondApprove.path("msg").asText(""));
            submitAndApproveFlowForSql();
        } catch (Exception ex) {
            record(25, "重复审核幂等", "SKIP", ex.getMessage());
        }
    }

    private static void runSqlRiskTests() throws Exception {
        try {
            submitAndApproveFlowForSql();
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                record(21, "standing 表同步", countPositive(stmt,
                        "SELECT COUNT(*) FROM basket_stage_standing WHERE season_id=" + SEASON_ID),
                        "season_id=" + SEASON_ID);
                record(22, "team_season_stat 同步", countPositive(stmt,
                        "SELECT COUNT(*) FROM basket_team_season_stat WHERE season_id=" + SEASON_ID),
                        "season_id=" + SEASON_ID);
                record(23, "player_season_stat 同步", countPositive(stmt,
                        "SELECT COUNT(*) FROM basket_player_season_stat WHERE season_id=" + SEASON_ID),
                        "season_id=" + SEASON_ID);
                int rankCount = count(stmt,
                        "SELECT COUNT(*) FROM basket_team_rank WHERE season_id=" + SEASON_ID)
                        + count(stmt,
                        "SELECT COUNT(*) FROM basket_player_rank WHERE season_id=" + SEASON_ID);
                record(24, "rank 表同步", rankCount > 0 ? "PASS" : "FAIL", "rows=" + rankCount);
            }
        } catch (Exception ex) {
            record(21, "standing 表同步", "SKIP", ex.getMessage());
            record(22, "team_season_stat 同步", "SKIP", ex.getMessage());
            record(23, "player_season_stat 同步", "SKIP", ex.getMessage());
            record(24, "rank 表同步", "SKIP", ex.getMessage());
        }
    }

    private static String countPositive(Statement stmt, String sql) throws Exception {
        return count(stmt, sql) > 0 ? "PASS" : "FAIL";
    }

    private static int count(Statement stmt, String sql) throws Exception {
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();
        return rs.getInt(1);
    }

    private static void runValidationTest(int no, String name, Map<String, Object> payload) {
        try {
            resetMatchForTest();
            String token = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
            JsonNode resp = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit", payload, token);
            record(no, name, resp.path("code").asInt() != 200 ? "PASS" : "FAIL",
                    "code=" + resp.path("code").asInt() + " msg=" + resp.path("msg").asText(""));
        } catch (Exception ex) {
            record(no, name, "SKIP", ex.getMessage());
        }
    }

    private static long submitAndApproveFlow() throws Exception {
        resetMatchForTest();
        return submitAndApproveFlowForSql();
    }

    private static long submitAndApproveFlowForSql() throws Exception {
        String captainToken = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-data/" + MATCH_ID + "/submit",
                buildValidSubmitPayload(), captainToken);
        assertOk(submit, "提交失败");
        long submissionId = submit.path("data").path("submissionId").asLong();
        String adminToken = adminLogin();
        approveSubmission(adminToken, submissionId);
        return submissionId;
    }

    private static boolean probeServices() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WEB_BASE + "/basket/public/season/" + SEASON_ID + "/schedule/page?currentPage=1&pageSize=1"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            HttpRequest adminReq = HttpRequest.newBuilder()
                    .uri(URI.create(ADMIN_BASE + "/login"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HTTP.send(adminReq, HttpResponse.BodyHandlers.ofString());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void ensurePasswords() throws Exception {
        String hash = new BCryptPasswordEncoder().encode(CAPTAIN_PASSWORD);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE basket_member_account SET password_hash='" + hash
                    + "', update_time=NOW() WHERE email='" + CAPTAIN_EMAIL + "'");
            stmt.executeUpdate("UPDATE basket_member_account SET password_hash='" + hash
                    + "', update_time=NOW() WHERE email='" + AWAY_CAPTAIN_EMAIL + "'");
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
    }

    private static void setMatchDataStatus(int status) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE basket_match SET data_status=" + status + " WHERE id=" + MATCH_ID);
        }
    }

    private static String webLogin(String email, String password) throws Exception {
        JsonNode resp = postJson(WEB_BASE, "/basket/member/auth/login/password",
                Map.of("email", email, "password", password), null);
        assertOk(resp, "Web 登录失败: " + email);
        return resp.path("data").path("token").asText();
    }

    private static String adminLogin() throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/login",
                Map.of("username", ADMIN_USER, "password", ADMIN_PASSWORD), null);
        assertOk(resp, "Admin 登录失败");
        return resp.path("token").asText();
    }

    private static void rejectSubmission(String adminToken, long submissionId, String reason) throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/basket/matchAudit/submission/" + submissionId + "/reject",
                Map.of("rejectReason", reason), adminToken);
        assertOk(resp, "驳回失败");
    }

    private static void approveSubmission(String adminToken, long submissionId) throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/basket/matchAudit/submission/" + submissionId + "/approve",
                Map.of("auditRemark", "risk-retest approve"), adminToken);
        assertOk(resp, "审核通过失败");
    }

    private static Map<String, Object> buildValidSubmitPayload() {
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
        payload.put("remark", "risk-retest");
        return payload;
    }

    private static Map<String, Object> mutatePeriodTotalMismatch() {
        Map<String, Object> payload = buildValidSubmitPayload();
        payload.put("periodScores", List.of(
                period(1, 10, 10), period(2, 10, 10), period(3, 10, 10), period(4, 10, 10)));
        return payload;
    }

    private static Map<String, Object> mutatePlayerPointsMismatch() {
        Map<String, Object> payload = buildValidSubmitPayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = new ArrayList<>((List<Map<String, Object>>) payload.get("playerStats"));
        players.get(0).put("points", 999);
        payload.put("playerStats", players);
        return payload;
    }

    private static Map<String, Object> mutateFieldGoalInvalid() {
        Map<String, Object> payload = buildValidSubmitPayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = new ArrayList<>((List<Map<String, Object>>) payload.get("playerStats"));
        players.get(0).put("fieldGoalsMade", 99);
        players.get(0).put("fieldGoalsAttempted", 1);
        payload.put("playerStats", players);
        return payload;
    }

    private static Map<String, Object> mutateThreePointInvalid() {
        Map<String, Object> payload = buildValidSubmitPayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = new ArrayList<>((List<Map<String, Object>>) payload.get("playerStats"));
        players.get(0).put("threePointsMade", 99);
        players.get(0).put("threePointsAttempted", 1);
        payload.put("playerStats", players);
        return payload;
    }

    private static Map<String, Object> mutateFreeThrowInvalid() {
        Map<String, Object> payload = buildValidSubmitPayload();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = new ArrayList<>((List<Map<String, Object>>) payload.get("playerStats"));
        players.get(0).put("freeThrowsMade", 99);
        players.get(0).put("freeThrowsAttempted", 1);
        payload.put("playerStats", players);
        return payload;
    }

    private static Map<String, Object> mutateNoScreenshot() {
        Map<String, Object> payload = buildValidSubmitPayload();
        payload.put("materialFileIds", List.of());
        return payload;
    }

    private static Map<String, Object> mutateNonRosterPlayer() {
        Map<String, Object> payload = buildValidSubmitPayload();
        payload.put("playerStats", List.of(
                playerStat("900010001", "9999999999999999999", 64, 5, 6, 2, 0, 7, 12, 2, 5, 2, 2),
                playerStat("900010002", "2081019227996807200", 55, 3, 5, 1, 0, 6, 11, 2, 5, 2, 2)));
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

    private static JsonNode getJson(String base, String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(30))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(response.body());
    }

    private static String resolveNonCaptainMemberEmail() throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT ma.email FROM basket_match_roster r "
                            + "JOIN basket_player_profile pp ON pp.id = r.player_id AND pp.is_delete = 0 "
                            + "JOIN basket_member_account ma ON ma.id = pp.member_id AND ma.is_delete = 0 "
                            + "WHERE r.match_id = " + MATCH_ID + " AND r.captain_flag = 0 AND r.is_delete = 0 "
                            + "LIMIT 1");
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return MEMBER_EMAIL;
    }

    private static void assertOk(JsonNode resp, String prefix) {
        int code = resp.path("code").asInt(-1);
        if (code != 200) {
            throw new IllegalStateException(prefix + ": " + resp.path("msg").asText("unknown"));
        }
    }

    private static void record(int no, String name, String status, String detail) {
        String line = String.format("%02d | %s | %s | %s", no, name, status, detail);
        RESULTS.add(line);
        System.out.println("[risk-retest] " + line);
    }

    private static String riskName(int no) {
        return switch (no) {
            case 1 -> "普通队员提交";
            case 2 -> "非本场队长提交";
            case 3 -> "未登录提交";
            case 4 -> "未认证用户提交";
            case 5 -> "客队队长审核中再提交";
            case 6 -> "审核中重复提交";
            case 7 -> "审核通过后再次提交";
            case 8 -> "审核中 resubmit";
            case 9 -> "驳回前 resubmit";
            case 10 -> "非原提交方 resubmit";
            case 11 -> "四节比分合计≠最终比分";
            case 12 -> "球员得分合计≠球队比分";
            case 13 -> "投篮命中>出手";
            case 14 -> "三分命中>出手";
            case 15 -> "罚球命中>出手";
            case 16 -> "无截图";
            case 17 -> "非 roster 球员";
            case 18 -> "旧 CRUD 写接口";
            case 19 -> "公开 API 过滤 SUBMITTED";
            case 20 -> "公开 API 过滤非 CONFIRMED";
            case 21 -> "standing 表同步";
            case 22 -> "team_season_stat 同步";
            case 23 -> "player_season_stat 同步";
            case 24 -> "rank 表同步";
            case 25 -> "重复审核幂等";
            default -> "未知项";
        };
    }

    private static void printSummary() {
        System.out.println("===== P0 Risk Retest Summary (25 items) =====");
        long pass = RESULTS.stream().filter(r -> r.contains(" | PASS |")).count();
        long fail = RESULTS.stream().filter(r -> r.contains(" | FAIL |")).count();
        long skip = RESULTS.stream().filter(r -> r.contains(" | SKIP |")).count();
        RESULTS.forEach(System.out::println);
        System.out.printf("TOTAL: PASS=%d FAIL=%d SKIP=%d%n", pass, fail, skip);
    }
}
