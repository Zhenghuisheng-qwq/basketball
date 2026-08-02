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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 为指定日期已完赛但无比分、且无待审提交的比赛，批量写入比分并由主队队长提交（审核中）。
 *
 * <pre>
 * mvn -pl basketball-admin -Dtest=com.zhs.basket.tools.Aug1PendingMatchDataSubmitRunner test -DfailIfNoTests=false
 * </pre>
 */
public final class Aug1PendingMatchDataSubmitRunner {

    private static final String JDBC_URL = System.getProperty(
            "migration.jdbc.url",
            "jdbc:mysql://192.168.31.47:3306/basketball?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8");
    private static final String JDBC_USER = System.getProperty("migration.jdbc.user", "root");
    private static final String JDBC_PASSWORD = System.getProperty("migration.jdbc.password", "123456");
    private static final String WEB_BASE = System.getProperty("integration.web.base", "http://127.0.0.1:8881");
    private static final String MATCH_DATE = System.getProperty("batch.match.date", "2026-08-01");
    private static final String CAPTAIN_PASSWORD = System.getProperty("integration.captain.password", "Test1234");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    /** matchId -> [homeScore, awayScore] */
    private static final Map<Long, int[]> SCORES = Map.of(
            900020084L, new int[] {78, 62},
            900020085L, new int[] {85, 71},
            900020086L, new int[] {92, 58},
            900020087L, new int[] {76, 68},
            900020088L, new int[] {88, 74}
    );

    public static void main(String[] args) throws Exception {
        if (!probeWeb()) {
            throw new IllegalStateException("basketball-web 未启动: " + WEB_BASE);
        }
        ensureCaptainPasswords();
        List<MatchTask> tasks = loadTasks();
        if (tasks.isEmpty()) {
            System.out.println("[aug1-batch] 无待处理比赛");
            return;
        }
        System.out.printf("[aug1-batch] 待处理 %d 场%n", tasks.size());
        int ok = 0;
        int fail = 0;
        for (MatchTask task : tasks) {
            try {
                prepareMatchScore(task);
                String token = webLogin(task.captainEmail);
                Map<String, Object> payload = buildTeamPlayerStatsPayload(task);
                JsonNode resp = postJson("/basket/player/match-data/" + task.matchId + "/team-player-stats/submit",
                        payload, token);
                if (resp.path("code").asInt() != 200) {
                    fail++;
                    System.out.printf("[aug1-batch] FAIL matchId=%d msg=%s%n",
                            task.matchId, resp.path("msg").asText(""));
                    continue;
                }
                ok++;
                System.out.printf("[aug1-batch] OK matchId=%d %s vs %s score=%d-%d submissionId=%s submissionNo=%s%n",
                        task.matchId, task.homeTeamName, task.awayTeamName, task.homeScore, task.awayScore,
                        resp.path("data").path("submissionId").asText(),
                        resp.path("data").path("submissionNo").asText());
            } catch (Exception ex) {
                fail++;
                System.out.printf("[aug1-batch] FAIL matchId=%d error=%s%n", task.matchId, ex.getMessage());
            }
        }
        System.out.printf("[aug1-batch] 完成 OK=%d FAIL=%d%n", ok, fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static List<MatchTask> loadTasks() throws Exception {
        List<MatchTask> tasks = new ArrayList<>();
        String sql = """
                SELECT m.id AS match_id, m.match_no, m.home_team_id, m.away_team_id,
                       ht.team_name AS home_team_name, at.team_name AS away_team_name,
                       tr.captain_member_id, ma.email AS captain_email
                FROM basket_match m
                JOIN basket_tournament_registration tr
                  ON tr.team_id = m.home_team_id AND tr.season_id = m.season_id AND tr.is_delete = 0
                JOIN basket_member_account ma ON ma.id = tr.captain_member_id AND ma.is_delete = 0
                LEFT JOIN basket_team ht ON ht.id = m.home_team_id
                LEFT JOIN basket_team at ON at.id = m.away_team_id
                WHERE DATE(m.scheduled_start_time) = ?
                  AND m.home_score = 0 AND m.away_score = 0
                  AND m.is_delete = 0
                  AND m.match_status <> 5
                  AND NOT EXISTS (
                    SELECT 1 FROM basket_match_data_submission s
                    WHERE s.match_id = m.id AND s.is_delete = 0
                      AND s.submission_status IN (0, 1)
                  )
                ORDER BY m.id
                """;
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, MATCH_DATE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long matchId = rs.getLong("match_id");
                    int[] score = SCORES.get(matchId);
                    if (score == null) {
                        continue;
                    }
                    MatchTask task = new MatchTask();
                    task.matchId = matchId;
                    task.matchNo = rs.getString("match_no");
                    task.homeTeamId = rs.getLong("home_team_id");
                    task.awayTeamId = rs.getLong("away_team_id");
                    task.homeTeamName = rs.getString("home_team_name");
                    task.awayTeamName = rs.getString("away_team_name");
                    task.captainMemberId = rs.getLong("captain_member_id");
                    task.captainEmail = rs.getString("captain_email");
                    task.homeScore = score[0];
                    task.awayScore = score[1];
                    task.rosterPlayers = loadHomeRoster(conn, task.homeTeamId, matchId);
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    private static List<RosterPlayer> loadHomeRoster(Connection conn, long teamId, long matchId) throws Exception {
        List<RosterPlayer> players = new ArrayList<>();
        String sql = """
                SELECT r.id AS roster_id, r.player_id
                FROM basket_tournament_season_roster r
                JOIN basket_match m ON m.home_team_id = r.team_id AND m.season_id = r.season_id
                WHERE m.id = ? AND r.team_id = ? AND r.roster_status = 1 AND r.is_delete = 0
                ORDER BY r.id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, matchId);
            ps.setLong(2, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RosterPlayer p = new RosterPlayer();
                    p.rosterId = rs.getLong("roster_id");
                    p.playerId = rs.getLong("player_id");
                    players.add(p);
                }
            }
        }
        if (players.isEmpty()) {
            throw new IllegalStateException("主队赛事阵容为空: matchId=" + matchId);
        }
        return players;
    }

    private static void prepareMatchScore(MatchTask task) throws Exception {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE basket_match
                     SET match_status = 3,
                         home_score = ?,
                         away_score = ?,
                         winner_team_id = CASE WHEN ? > ? THEN home_team_id WHEN ? > ? THEN away_team_id ELSE NULL END,
                         actual_end_time = COALESCE(actual_end_time, DATE_ADD(scheduled_start_time, INTERVAL 2 HOUR)),
                         update_time = NOW()
                     WHERE id = ? AND home_score = 0 AND away_score = 0
                     """)) {
            ps.setInt(1, task.homeScore);
            ps.setInt(2, task.awayScore);
            ps.setInt(3, task.homeScore);
            ps.setInt(4, task.awayScore);
            ps.setInt(5, task.awayScore);
            ps.setInt(6, task.homeScore);
            ps.setLong(7, task.matchId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("更新比赛比分失败: matchId=" + task.matchId);
            }
        }
    }

    private static Map<String, Object> buildTeamPlayerStatsPayload(MatchTask task) {
        int[] points = splitScore(task.homeScore, task.rosterPlayers.size());
        List<Map<String, Object>> playerStats = new ArrayList<>();
        for (int i = 0; i < task.rosterPlayers.size(); i++) {
            RosterPlayer player = task.rosterPlayers.get(i);
            int pts = points[i];
            Map<String, Object> item = new HashMap<>();
            item.put("playerId", String.valueOf(player.playerId));
            item.put("rosterId", String.valueOf(player.rosterId));
            item.put("points", pts);
            item.put("rebounds", 2 + (i % 4));
            item.put("assists", 1 + (i % 5));
            item.put("steals", i % 2);
            item.put("blocks", i % 3);
            item.put("turnovers", 1 + (i % 3));
            item.put("fouls", 1 + (i % 4));
            playerStats.add(item);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("submitterTeamId", String.valueOf(task.homeTeamId));
        payload.put("playerStats", playerStats);
        payload.put("remark", "8月1日批量补录，待管理员审核");
        return payload;
    }

    private static int[] splitScore(int total, int count) {
        int[] result = new int[count];
        int base = total / count;
        int remainder = total % count;
        for (int i = 0; i < count; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }

    private static void ensureCaptainPasswords() throws Exception {
        String hash = new BCryptPasswordEncoder().encode(CAPTAIN_PASSWORD);
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT DISTINCT ma.email
                     FROM basket_match m
                     JOIN basket_tournament_registration tr
                       ON tr.team_id = m.home_team_id AND tr.season_id = m.season_id AND tr.is_delete = 0
                     JOIN basket_member_account ma ON ma.id = tr.captain_member_id AND ma.is_delete = 0
                     WHERE DATE(m.scheduled_start_time) = '%s'
                       AND m.home_score = 0 AND m.away_score = 0 AND m.is_delete = 0
                     """.formatted(MATCH_DATE))) {
            while (rs.next()) {
                String email = rs.getString(1);
                stmt.executeUpdate("UPDATE basket_member_account SET password_hash='" + hash
                        + "', update_time=NOW() WHERE email='" + email.replace("'", "''") + "'");
            }
        }
    }

    private static boolean probeWeb() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WEB_BASE + "/basket/public/season/1/schedule/page?currentPage=1&pageSize=1"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String webLogin(String email) throws Exception {
        JsonNode resp = postJson("/basket/member/auth/login/password",
                Map.of("email", email, "password", CAPTAIN_PASSWORD), null);
        if (resp.path("code").asInt() != 200) {
            throw new IllegalStateException("登录失败 " + email + ": " + resp.path("msg").asText());
        }
        return resp.path("data").path("token").asText();
    }

    private static JsonNode postJson(String path, Object body, String token) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(WEB_BASE + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(response.body());
    }

    private static final class MatchTask {
        long matchId;
        String matchNo;
        long homeTeamId;
        long awayTeamId;
        String homeTeamName;
        String awayTeamName;
        long captainMemberId;
        String captainEmail;
        int homeScore;
        int awayScore;
        List<RosterPlayer> rosterPlayers;
    }

    private static final class RosterPlayer {
        long rosterId;
        long playerId;
    }
}
