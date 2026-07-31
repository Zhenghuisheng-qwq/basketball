package com.zhs.basket.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * CNPA 第四轮-B：M06 数据争议 + M07 弃赛异常 HTTP/JDBC 联调。
 * 需 basketball-web:8881、basketball-admin:8080 已启动。
 */
public final class CnpaDisputeExceptionP1IntegrationRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");
    private static final String WEB_BASE = System.getProperty("integration.web.base", "http://127.0.0.1:8881");
    private static final String ADMIN_BASE = System.getProperty("integration.admin.base", "http://127.0.0.1:8080");
    private static final long DISPUTE_MATCH_ID = Long.parseLong(System.getProperty("integration.dispute.match.id", "900020001"));
    private static final long EXCEPTION_MATCH_ID = Long.parseLong(System.getProperty("integration.exception.match.id", "900020002"));
    private static final long POSTPONE_MATCH_ID = Long.parseLong(System.getProperty("integration.postpone.match.id", "900020003"));
    private static final long CANCEL_MATCH_ID = Long.parseLong(System.getProperty("integration.cancel.match.id", "900020004"));
    private static final long MATERIAL_FILE_ID = Long.parseLong(System.getProperty("integration.material.fileId", "2081011198194663426"));
    private static final String RNG_PLAYER_P1 = "2081010979671425026";
    private static final long RNG_TEAM = 900010001L;
    private static final long PHX_TEAM = 900010002L;
    private static final String CAPTAIN_EMAIL = System.getProperty("integration.captain.email", "111@qq.com");
    private static final String CAPTAIN_PASSWORD = System.getProperty("integration.captain.password", "Test1234");
    private static final String ADMIN_USER = System.getProperty("integration.admin.user", "admin");
    private static final String ADMIN_PASSWORD = System.getProperty("integration.admin.password", "admin123");

    private static final ObjectMapper MAPPER = createMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private static final List<String> RESULTS = new ArrayList<>();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(module);
        return mapper;
    }

    public static void main(String[] args) throws Exception {
        if (!probeServices()) {
            record("M06/M07", "SKIP", "web/admin 未启动");
            printSummary();
            return;
        }
        runSqlFile("cnpa-match-data-dispute-exception-p1-permissions-fix.sql");
        String captainToken = webLogin(CAPTAIN_EMAIL, CAPTAIN_PASSWORD);
        String adminToken = adminLogin();
        runSqlFile("cnpa-match-data-restore-confirmed-900020001.sql");
        runSqlFile("cnpa-match-data-p1-integration-prep.sql");
        verifyPermissions();

        runM06RejectFlow(captainToken, adminToken);
        runSqlFile("cnpa-match-data-p1-integration-prep.sql");
        runM06CorrectionFlow(captainToken, adminToken);
        runM07Flows(captainToken, adminToken);
        runM07ConfirmedBlocked(captainToken);

        printSummary();
    }

    private static void restoreConfirmedMatch(String captainToken, String adminToken) throws Exception {
        int status = queryInt("SELECT data_status FROM basket_match WHERE id=" + DISPUTE_MATCH_ID);
        if (status == 2) {
            record("恢复CONFIRMED比赛", "PASS", "已是 CONFIRMED");
            return;
        }
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE basket_match SET data_status=0, home_score=0, away_score=0, winner_team_id=NULL WHERE id="
                    + DISPUTE_MATCH_ID);
            stmt.execute("DELETE FROM basket_match_data_submission WHERE match_id=" + DISPUTE_MATCH_ID);
        }
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-data/" + DISPUTE_MATCH_ID + "/submit",
                buildSubmitPayload(), captainToken);
        assertOk(submit, "恢复提交");
        long submissionId = submit.path("data").path("submissionId").asLong();
        JsonNode approve = postJson(ADMIN_BASE, "/basket/matchAudit/submission/" + submissionId + "/approve",
                Map.of("auditRemark", "P1联调恢复CONFIRMED"), adminToken);
        assertOk(approve, "恢复审核");
        record("恢复CONFIRMED比赛", "PASS", "submissionId=" + submissionId);
    }

    private static Map<String, Object> buildSubmitPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("homeScore", 64);
        payload.put("awayScore", 55);
        payload.put("periodScores", List.of(
                period(1, 17, 23), period(2, 17, 8), period(3, 16, 15), period(4, 14, 9)));
        payload.put("teamStats", List.of(
                teamStat(RNG_TEAM, 64, 20, 19, 6, 3, 0, 0, 26, 46, 5, 15, 7, 9),
                teamStat(PHX_TEAM, 55, 9, 10, 3, 2, 0, 0, 23, 28, 6, 8, 3, 3)));
        payload.put("playerStats", List.of(
                playerStat(RNG_TEAM, Long.parseLong(RNG_PLAYER_P1), 18, 5, 6, 2, 0, 7, 12, 2, 5, 2, 2),
                playerStat(RNG_TEAM, 2081041427005038596L, 14, 8, 2, 1, 1, 6, 10, 0, 1, 2, 3),
                playerStat(RNG_TEAM, 2081072741716336642L, 12, 4, 4, 1, 1, 5, 9, 1, 3, 1, 2),
                playerStat(RNG_TEAM, 2081073127508418562L, 10, 2, 3, 1, 1, 4, 8, 2, 4, 0, 0),
                playerStat(RNG_TEAM, 2081073380093599747L, 10, 1, 4, 1, 0, 4, 7, 0, 2, 2, 2),
                playerStat(PHX_TEAM, 2081019227996807169L, 20, 3, 5, 1, 0, 8, 11, 4, 5, 0, 0),
                playerStat(PHX_TEAM, 2082478335308455940L, 18, 4, 2, 1, 1, 7, 9, 2, 3, 2, 2),
                playerStat(PHX_TEAM, 2082786571525836803L, 17, 2, 3, 1, 0, 8, 8, 0, 0, 1, 1)));
        payload.put("materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID)));
        payload.put("remark", "p1-restore");
        return payload;
    }

    private static void runM06RejectFlow(String captainToken, String adminToken) throws Exception {
        JsonNode avail = getJson(WEB_BASE, "/basket/player/match-dispute/" + DISPUTE_MATCH_ID + "/available", captainToken);
        boolean canDispute = avail.path("data").path("canDispute").asBoolean(false);
        record("M06-可用窗口", canDispute ? "PASS" : "FAIL",
                "canDispute=" + canDispute + " reason=" + avail.path("data").path("cannotReason").asText(""));

        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-dispute/" + DISPUTE_MATCH_ID + "/submit",
                Map.of(
                        "disputeType", "STATS",
                        "disputeReason", "RNG-P1 助攻应为 7，不是 6（驳回测试）",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))
                ), captainToken);
        assertOk(submit, "争议提交");
        long disputeId = Long.parseLong(submit.path("data").path("disputeId").asText());
        record("M06-提交争议", "PASS", "disputeId=" + disputeId);

        JsonNode dup = postJson(WEB_BASE, "/basket/player/match-dispute/" + DISPUTE_MATCH_ID + "/submit",
                Map.of("disputeType", "STATS", "disputeReason", "重复", "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))),
                captainToken);
        record("M06-重复争议", dup.path("code").asInt() != 200 ? "PASS" : "FAIL", "code=" + dup.path("code").asInt());

        assertSqlCount("basket_match_data_dispute_material", "match_id=" + DISPUTE_MATCH_ID, 1, "M06-材料入库");
        assertActiveSlot("basket_match_data_dispute", DISPUTE_MATCH_ID, "M06-active_dispute_slot");

        JsonNode detail = getJson(ADMIN_BASE, "/basket/matchDispute/" + disputeId, adminToken);
        record("M06-Admin详情", detail.path("code").asInt() == 200 ? "PASS" : "FAIL", "materials="
                + detail.path("data").path("materials").size());

        JsonNode reject = postJson(ADMIN_BASE, "/basket/matchDispute/" + disputeId + "/reject",
                Map.of("handleRemark", "材料不足，驳回测试"), adminToken);
        assertOk(reject, "争议驳回");
        assertSql("SELECT dispute_status FROM basket_match_data_dispute WHERE id=" + disputeId, "REJECTED", "M06-驳回状态");
        int assistsBefore = queryInt("SELECT assists FROM basket_match_player_stat WHERE match_id=" + DISPUTE_MATCH_ID
                + " AND player_id=" + RNG_PLAYER_P1);
        record("M06-驳回不改数据", assistsBefore == 6 ? "PASS" : "FAIL", "assists=" + assistsBefore);
    }

    private static void runM06CorrectionFlow(String captainToken, String adminToken) throws Exception {
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-dispute/" + DISPUTE_MATCH_ID + "/submit",
                Map.of(
                        "disputeType", "STATS",
                        "disputeReason", "RNG-P1 助攻应为 7，不是 6",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))
                ), captainToken);
        assertOk(submit, "争议提交(修正流)");
        long disputeId = Long.parseLong(submit.path("data").path("disputeId").asText());

        Map<String, Object> correction = buildCorrectionPayload(7);
        correction.put("handleRemark", "采纳争议，修正 RNG-P1 助攻");
        correction.put("correctionReason", "争议采纳修正 assists 6→7");
        validateCorrectionPayloadLocally(correction);

        JsonNode uphold = postJson(ADMIN_BASE, "/basket/matchDispute/" + disputeId + "/apply-correction", correction, adminToken);
        assertOk(uphold, "争议采纳修正");
        record("M06-apply-correction", "PASS", "disputeId=" + disputeId);

        assertSql("SELECT dispute_status FROM basket_match_data_dispute WHERE id=" + disputeId, "UPHELD", "M06-UPHELD");
        assertSqlCount("basket_match_data_revision", "match_id=" + DISPUTE_MATCH_ID, 1, "M06-revision", true);
        assertSqlCount("basket_stat_change_record", "biz_type='MATCH' AND biz_id=" + DISPUTE_MATCH_ID, 1, "M06-stat_change", true);
        int assists = queryInt("SELECT assists FROM basket_match_player_stat WHERE match_id=" + DISPUTE_MATCH_ID
                + " AND player_id=" + RNG_PLAYER_P1);
        record("M06-球员助攻修正", assists == 7 ? "PASS" : "FAIL", "assists=" + assists);
        assertSqlCount("basket_stat_sync_record", "match_id=" + DISPUTE_MATCH_ID + " AND sync_type='DISPUTE_UPHELD'", 1, "M06-stat_sync", true);

        JsonNode status = getJson(WEB_BASE, "/basket/player/match-dispute/" + DISPUTE_MATCH_ID + "/status", captainToken);
        String st = status.path("data").path("disputeStatus").asText("");
        record("M06-Web状态", "UPHELD".equals(st) ? "PASS" : "FAIL", "status=" + st);
    }

    private static void runM07Flows(String captainToken, String adminToken) throws Exception {
        runM07Forfeit(captainToken, adminToken);
        runM07Postpone(captainToken, adminToken);
        runM07Cancel(captainToken, adminToken);
    }

    private static void runM07Forfeit(String captainToken, String adminToken) throws Exception {
        JsonNode avail = getJson(WEB_BASE, "/basket/player/match-exception/" + EXCEPTION_MATCH_ID + "/available", captainToken);
        record("M07-FORFEIT可用", avail.path("data").path("canSubmitException").asBoolean(false) ? "PASS" : "FAIL",
                avail.path("data").path("cannotReason").asText(""));

        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-exception/" + EXCEPTION_MATCH_ID + "/submit",
                Map.of("exceptionType", "FORFEIT", "exceptionReason", "对方未按时参赛",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))), captainToken);
        assertOk(submit, "异常提交");
        long appId = submit.path("data").path("applicationId").asLong();
        record("M07-提交FORFEIT", "PASS", "applicationId=" + appId);

        JsonNode dup = postJson(WEB_BASE, "/basket/player/match-exception/" + EXCEPTION_MATCH_ID + "/submit",
                Map.of("exceptionType", "FORFEIT", "exceptionReason", "重复", "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))),
                captainToken);
        record("M07-重复异常", dup.path("code").asInt() != 200 ? "PASS" : "FAIL", "code=" + dup.path("code").asInt());

        assertSqlCount("basket_match_exception_material", "match_id=" + EXCEPTION_MATCH_ID, 1, "M07-材料");
        assertActiveSlot("basket_match_exception_application", EXCEPTION_MATCH_ID, "M07-active_application_slot");

        JsonNode decide = postJson(ADMIN_BASE, "/basket/matchException/" + appId + "/decide",
                Map.of("decisionType", "FORFEIT", "walkoverTeamId", String.valueOf(PHX_TEAM), "decisionResult", "PHX 弃赛判负 20-0"),
                adminToken);
        assertOk(decide, "FORFEIT裁定");

        assertSql("SELECT application_status FROM basket_match_exception_application WHERE id=" + appId, "APPROVED", "M07-FORFEIT状态");
        assertSqlCount("basket_match_exception_audit", "application_id=" + appId, 1, "M07-audit", true);
        int home = queryInt("SELECT home_score FROM basket_match WHERE id=" + EXCEPTION_MATCH_ID);
        int away = queryInt("SELECT away_score FROM basket_match WHERE id=" + EXCEPTION_MATCH_ID);
        record("M07-20:0判负", home == 20 && away == 0 ? "PASS" : "FAIL", "score=" + home + ":" + away);
        assertSqlCount("basket_match_official_result", "match_id=" + EXCEPTION_MATCH_ID, 1, "M07-official_result", true);
        assertSqlCount("basket_stat_sync_record", "match_id=" + EXCEPTION_MATCH_ID + " AND sync_type='EXCEPTION_DECISION_FORFEIT'", 1, "M07-stat_sync", true);
    }

    private static void runM07Postpone(String captainToken, String adminToken) throws Exception {
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-exception/" + POSTPONE_MATCH_ID + "/submit",
                Map.of("exceptionType", "POSTPONE", "exceptionReason", "场地冲突申请延期",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))), captainToken);
        assertOk(submit, "POSTPONE提交");
        long appId = submit.path("data").path("applicationId").asLong();
        JsonNode decide = postJson(ADMIN_BASE, "/basket/matchException/" + appId + "/decide",
                Map.of("decisionType", "POSTPONE", "scheduledStartTime", "2030-01-15 19:00:00", "decisionResult", "延期至2030-01-15"),
                adminToken);
        assertOk(decide, "POSTPONE裁定");
        int status = queryInt("SELECT match_status FROM basket_match WHERE id=" + POSTPONE_MATCH_ID);
        record("M07-POSTPONE", status == 4 ? "PASS" : "FAIL", "match_status=" + status);
    }

    private static void runM07Cancel(String captainToken, String adminToken) throws Exception {
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-exception/" + CANCEL_MATCH_ID + "/submit",
                Map.of("exceptionType", "CANCEL", "exceptionReason", "双方协商取消",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))), captainToken);
        assertOk(submit, "CANCEL提交");
        long appId = submit.path("data").path("applicationId").asLong();
        JsonNode decide = postJson(ADMIN_BASE, "/basket/matchException/" + appId + "/decide",
                Map.of("decisionType", "CANCEL", "decisionResult", "比赛取消"), adminToken);
        assertOk(decide, "CANCEL裁定");
        int status = queryInt("SELECT match_status FROM basket_match WHERE id=" + CANCEL_MATCH_ID);
        record("M07-CANCEL", status == 5 ? "PASS" : "FAIL", "match_status=" + status);
    }

    private static void runM07ConfirmedBlocked(String captainToken) throws Exception {
        JsonNode submit = postJson(WEB_BASE, "/basket/player/match-exception/" + DISPUTE_MATCH_ID + "/submit",
                Map.of("exceptionType", "FORFEIT", "exceptionReason", "已确认比赛不应可提交",
                        "materialFileIds", List.of(String.valueOf(MATERIAL_FILE_ID))), captainToken);
        record("M07-CONFIRMED拦截", submit.path("code").asInt() != 200 ? "PASS" : "FAIL",
                "code=" + submit.path("code").asInt() + " msg=" + submit.path("msg").asText(""));
    }

    @SuppressWarnings("unchecked")
    private static void validateCorrectionPayloadLocally(Map<String, Object> correction) {
        List<Map<String, Object>> teamStats = (List<Map<String, Object>>) correction.get("teamStats");
        List<Map<String, Object>> playerStats = (List<Map<String, Object>>) correction.get("playerStats");
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(RNG_TEAM, new int[10]);
        teamAgg.put(PHX_TEAM, new int[10]);
        String[] labels = {"points", "rebounds", "assists", "steals", "blocks", "turnovers", "fouls",
                "fieldGoalsMade", "threePointsMade", "freeThrowsMade"};
        for (Map<String, Object> ps : playerStats) {
            long teamId = Long.parseLong(String.valueOf(ps.get("teamId")));
            int[] agg = teamAgg.get(teamId);
            if (agg == null) {
                throw new IllegalStateException("unknown player teamId=" + teamId);
            }
            agg[0] += intVal(ps.get("points"));
            agg[1] += intVal(ps.get("rebounds"));
            agg[2] += intVal(ps.get("assists"));
            agg[3] += intVal(ps.get("steals"));
            agg[4] += intVal(ps.get("blocks"));
            agg[5] += intVal(ps.get("turnovers"));
            agg[6] += intVal(ps.get("fouls"));
            agg[7] += intVal(ps.get("fieldGoalsMade"));
            agg[8] += intVal(ps.get("threePointsMade"));
            agg[9] += intVal(ps.get("freeThrowsMade"));
        }
        for (Map<String, Object> ts : teamStats) {
            long teamId = Long.parseLong(String.valueOf(ts.get("teamId")));
            int[] agg = teamAgg.get(teamId);
            if (agg == null) {
                continue;
            }
            for (int i = 0; i < labels.length; i++) {
                int teamVal = intVal(ts.get(labels[i]));
                if (teamVal != agg[i]) {
                    throw new IllegalStateException("local validate mismatch team=" + teamId + " " + labels[i]
                            + " team=" + teamVal + " players=" + agg[i]);
                }
            }
        }
    }

    private static int intVal(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    private static Map<String, Object> buildCorrectionPayload(int p1Assists) throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("homeScore", 64);
        req.put("awayScore", 55);
        req.put("periodScores", List.of(
                period(1, 17, 23), period(2, 17, 8), period(3, 16, 15), period(4, 14, 9)));

        List<Map<String, Object>> teamStats = new ArrayList<>();
        List<Map<String, Object>> playerStats = new ArrayList<>();
        Map<Long, int[]> teamAgg = new HashMap<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet players = stmt.executeQuery(
                    "SELECT team_id, player_id, points, rebounds, assists, steals, blocks, turnovers, fouls,"
                            + " field_goals_made, field_goals_attempted, three_points_made, three_points_attempted,"
                            + " free_throws_made, free_throws_attempted"
                            + " FROM basket_match_player_stat WHERE match_id=" + DISPUTE_MATCH_ID + " AND is_delete=0");
            while (players.next()) {
                String playerIdStr = players.getString("player_id");
                long teamId = Long.parseLong(players.getString("team_id"));
                int assists = players.getInt("assists");
                if (RNG_PLAYER_P1.equals(playerIdStr)) {
                    assists = p1Assists;
                }
                int to = players.getInt("turnovers");
                int fouls = players.getInt("fouls");
                playerStats.add(playerStat(teamId, playerIdStr,
                        players.getInt("points"), players.getInt("rebounds"), assists,
                        players.getInt("steals"), players.getInt("blocks"), to, fouls,
                        players.getInt("field_goals_made"), players.getInt("field_goals_attempted"),
                        players.getInt("three_points_made"), players.getInt("three_points_attempted"),
                        players.getInt("free_throws_made"), players.getInt("free_throws_attempted")));
                int[] agg = teamAgg.computeIfAbsent(teamId, k -> new int[10]);
                agg[0] += players.getInt("points");
                agg[1] += players.getInt("rebounds");
                agg[2] += assists;
                agg[3] += players.getInt("steals");
                agg[4] += players.getInt("blocks");
                agg[5] += to;
                agg[6] += fouls;
                agg[7] += players.getInt("field_goals_made");
                agg[8] += players.getInt("three_points_made");
                agg[9] += players.getInt("free_throws_made");
            }
            ResultSet teams = stmt.executeQuery(
                    "SELECT team_id, points, rebounds, assists, steals, blocks, turnovers, fouls,"
                            + " field_goals_made, field_goals_attempted, three_points_made, three_points_attempted,"
                            + " free_throws_made, free_throws_attempted"
                            + " FROM basket_match_team_stat WHERE match_id=" + DISPUTE_MATCH_ID + " AND is_delete=0");
            while (teams.next()) {
                long teamId = Long.parseLong(teams.getString("team_id"));
                int[] agg = teamAgg.get(teamId);
                teamStats.add(teamStat(teamId,
                        teams.getInt("points"), agg != null ? agg[1] : teams.getInt("rebounds"),
                        agg != null ? agg[2] : teams.getInt("assists"),
                        agg != null ? agg[3] : teams.getInt("steals"),
                        agg != null ? agg[4] : teams.getInt("blocks"),
                        agg != null ? agg[5] : teams.getInt("turnovers"),
                        agg != null ? agg[6] : teams.getInt("fouls"),
                        agg != null ? agg[7] : teams.getInt("field_goals_made"),
                        teams.getInt("field_goals_attempted"),
                        agg != null ? agg[8] : teams.getInt("three_points_made"),
                        teams.getInt("three_points_attempted"),
                        agg != null ? agg[9] : teams.getInt("free_throws_made"),
                        teams.getInt("free_throws_attempted")));
            }
        }
        req.put("teamStats", teamStats);
        req.put("playerStats", playerStats);
        req.put("correctionReason", "RNG-P1 assists correction");
        return req;
    }

    private static void verifyPermissions() throws Exception {
        String[] perms = {
                "basket:matchDispute:list", "basket:matchDispute:query", "basket:matchDispute:reject",
                "basket:matchDispute:uphold", "basket:matchDispute:correct",
                "basket:matchException:list", "basket:matchException:query", "basket:matchException:reject",
                "basket:matchException:decide"
        };
        for (String p : perms) {
            int c = queryInt("SELECT COUNT(*) FROM sys_menu WHERE perms='" + p + "'");
            record("权限-" + p, c >= 1 ? "PASS" : "FAIL", "count=" + c);
        }
    }

    private static void runSqlFile(String fileName) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (root.getFileName().toString().equals("basketball-admin")) {
            root = root.getParent();
        }
        Path sql = root.resolve("docs/sql/" + fileName);
        String content = Files.readString(sql, StandardCharsets.UTF_8);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(content);
        }
        System.out.println("[prep] executed " + fileName);
    }

    private static boolean probeServices() {
        try {
            HttpRequest web = HttpRequest.newBuilder().uri(URI.create(WEB_BASE + "/basket/member/auth/login/password"))
                    .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)).build();
            HTTP.send(web, HttpResponse.BodyHandlers.ofString());
            HttpRequest admin = HttpRequest.newBuilder().uri(URI.create(ADMIN_BASE + "/login"))
                    .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)).build();
            HTTP.send(admin, HttpResponse.BodyHandlers.ofString());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String webLogin(String email, String password) throws Exception {
        JsonNode resp = postJson(WEB_BASE, "/basket/member/auth/login/password", Map.of("email", email, "password", password), null);
        assertOk(resp, "Web登录");
        return resp.path("data").path("token").asText();
    }

    private static String adminLogin() throws Exception {
        JsonNode resp = postJson(ADMIN_BASE, "/login", Map.of("username", ADMIN_USER, "password", ADMIN_PASSWORD), null);
        assertOk(resp, "Admin登录");
        return resp.path("token").asText();
    }

    private static void assertActiveSlot(String table, long matchId, String label) throws Exception {
        String col = table.contains("dispute") ? "active_dispute_slot" : "active_application_slot";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT " + col + " FROM " + table
                    + " WHERE match_id=" + matchId + " AND is_delete=0 ORDER BY id DESC LIMIT 1");
            if (!rs.next()) {
                record(label, "FAIL", "no row");
                return;
            }
            String slot = rs.getString(1);
            record(label, slot != null && !slot.isEmpty() ? "PASS" : "FAIL", "slot=" + slot);
        }
    }

    private static void assertSqlCount(String table, String where, int min, String label) throws Exception {
        assertSqlCount(table, where, min, label, false);
    }

    private static void assertSqlCount(String table, String where, int min, String label, boolean gte) throws Exception {
        int c = queryInt("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        boolean ok = gte ? c >= min : c == min;
        record(label, ok ? "PASS" : "FAIL", "count=" + c);
    }

    private static void assertSql(String sql, String expected, String label) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                record(label, "FAIL", "no row");
                return;
            }
            String val = rs.getString(1);
            record(label, expected.equals(val) ? "PASS" : "FAIL", "value=" + val);
        }
    }

    private static int queryInt(String sql) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Map<String, Object> period(int no, int home, int away) {
        return Map.of("periodNo", no, "periodType", "REGULAR", "homeScore", home, "awayScore", away);
    }

    private static Map<String, Object> teamStat(long teamId, int pts, int reb, int ast, int stl, int blk, int to, int fouls,
            int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        Map<String, Object> m = new HashMap<>();
        m.put("teamId", String.valueOf(teamId));
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

    private static Map<String, Object> playerStat(long teamId, long playerId, int pts, int reb, int ast, int stl, int blk,
            int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        return playerStat(teamId, String.valueOf(playerId), pts, reb, ast, stl, blk, 0, 0, fgm, fga, tpm, tpa, ftm, fta);
    }

    private static Map<String, Object> playerStat(long teamId, String playerId, int pts, int reb, int ast, int stl, int blk,
            int to, int fouls, int fgm, int fga, int tpm, int tpa, int ftm, int fta) {
        Map<String, Object> m = new HashMap<>();
        m.put("teamId", String.valueOf(teamId));
        m.put("playerId", playerId);
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

    private static JsonNode postJson(String base, String path, Object body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return MAPPER.readTree(HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    private static JsonNode getJson(String base, String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(base + path)).timeout(Duration.ofSeconds(30)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return MAPPER.readTree(HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    private static void assertOk(JsonNode resp, String prefix) {
        int code = resp.path("code").asInt(-1);
        if (code != 200) {
            throw new IllegalStateException(prefix + ": code=" + code + " msg=" + resp.path("msg").asText("unknown"));
        }
    }

    private static void record(String name, String status, String detail) {
        String line = name + " | " + status + " | " + detail;
        RESULTS.add(line);
        System.out.println("[p1-integration] " + line);
    }

    private static void printSummary() {
        System.out.println("===== P1 Integration Summary =====");
        long pass = RESULTS.stream().filter(r -> r.contains(" | PASS |")).count();
        long fail = RESULTS.stream().filter(r -> r.contains(" | FAIL |")).count();
        long skip = RESULTS.stream().filter(r -> r.contains(" | SKIP |")).count();
        RESULTS.forEach(System.out::println);
        System.out.printf("TOTAL: PASS=%d FAIL=%d SKIP=%d%n", pass, fail, skip);
    }
}
