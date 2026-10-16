package com.zhs.service.impl.v4;

import com.zhs.MiniApplication;
import com.zhs.common.UserContext;
import com.zhs.service.ITeamMemberBusinessService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MiniApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventRosterMembershipSyncLocalTest {

    private static final long SEASON_ID = 990801L;
    private static final long DIVISION_ID = 990813L;
    private static final long TEAM_ID = 990821L;
    private static final long REGISTRATION_ID = 990831L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ITeamMemberBusinessService teamMemberService;
    @Autowired
    private EventRosterMembershipSync rosterSync;

    private final List<Long> players = new ArrayList<>();

    @BeforeAll
    void assertLocalDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertSafeLocalUrl(url);
            assertEquals("basketball", connection.getCatalog());
        }
    }

    @AfterAll
    void cleanupFixture() {
        if (jdbc == null) {
            return;
        }
        cleanup();
    }

    @Test
    void leaveReleasesPreMatchRosterAndStartedMatchStaysUntouched() {
        cleanup();
        seed();
        try {
            Long leaver = players.get(1);
            Long memberId = jdbc.queryForObject(
                    "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, leaver);
            UserContext.setUserId(memberId);
            teamMemberService.leave(String.valueOf(TEAM_ID), "127.0.0.1");
            UserContext.clear();

            Integer released = jdbc.queryForObject("""
                    SELECT is_delete FROM basket_registration_player
                    WHERE registration_id = ? AND player_id = ?
                    """, Integer.class, REGISTRATION_ID, leaver);
            assertEquals(1, released);
            assertEquals(2, jdbc.queryForObject(
                    "SELECT lineup_count FROM basket_registration WHERE id = ?", Integer.class, REGISTRATION_ID));
            Integer audits = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM basket_registration_audit
                    WHERE registration_id = ? AND audit_action = 'MEMBER_LEFT' AND is_delete = 0
                    """, Integer.class, REGISTRATION_ID);
            assertTrue(audits != null && audits >= 1);

            Long stageId = jdbc.queryForObject("SELECT stage_id FROM basket_match LIMIT 1", Long.class);
            jdbc.update("""
                    INSERT INTO basket_match (season_id, division_id, match_no, stage_id, match_status, publish_status, is_delete, create_time, update_time)
                    VALUES (?, ?, 'E2E-WEB-M', ?, 'PLAYING', 'DRAFT', 0, NOW(), NOW())
                    """, SEASON_ID, DIVISION_ID, stageId);
            Long lockedPlayer = players.get(2);
            assertEquals(0, rosterSync.releaseFromWritableRegistrations(TEAM_ID, lockedPlayer, "开赛后不应释放"));
            Integer stillActive = jdbc.queryForObject("""
                    SELECT is_delete FROM basket_registration_player
                    WHERE registration_id = ? AND player_id = ?
                    """, Integer.class, REGISTRATION_ID, lockedPlayer);
            assertEquals(0, stillActive);
        } finally {
            UserContext.clear();
            cleanup();
        }
    }

    private void seed() {
        copyRow("basket_season", 184L, SEASON_ID, Map.of(
                "season_year", 2098,
                "season_name", "E2E-WEB",
                "display_title", "E2E Web",
                "match_start_time", Timestamp.valueOf("2026-10-01 00:00:00"),
                "match_end_time", Timestamp.valueOf("2026-12-31 00:00:00"),
                "publish_status", "DRAFT",
                "season_status", "NOT_STARTED"));
        copyRow("basket_division", 213L, DIVISION_ID, Map.of(
                "season_id", SEASON_ID,
                "division_name", "E2E Web Division",
                "division_status", "REGISTRATION_OPEN",
                "platform_code", "PS_XBOX"));
        copyRow("basket_team", 2108511641754677249L, TEAM_ID, Map.of(
                "team_no", "E2E-WEB-T", "team_name", "E2E Web", "short_name", "EW", "division_id", DIVISION_ID));
        players.clear();
        players.addAll(jdbc.query("""
                SELECT p.id FROM basket_player_profile p
                JOIN basket_player_platform_account a ON a.player_id = p.id AND a.is_delete = 0
                WHERE p.is_delete = 0 AND p.certification_status = 3 AND a.account_status = 1
                  AND a.platform IN ('PS', 'XBOX')
                GROUP BY p.id ORDER BY p.id LIMIT 3
                """, (rs, row) -> rs.getLong(1)));
        if (players.size() < 3) {
            throw new IllegalStateException("测试库可用球员不足");
        }
        for (int i = 0; i < players.size(); i++) {
            insertMember(players.get(i), i == 0);
        }
        Long captain = players.get(0);
        Long captainMember = jdbc.queryForObject(
                "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, captain);
        jdbc.update("""
                INSERT INTO basket_registration
                (id, season_id, division_id, registration_mode, registration_source, team_id, captain_player_id,
                 captain_member_id, registration_status, lineup_count, lineup_version, submit_time, approve_time,
                 create_time, update_time, is_delete)
                VALUES (?, ?, ?, 'TEAM', 'TEAM_DIRECT', ?, ?, ?, 'APPROVED', 3, 0, NOW(), NOW(), NOW(), NOW(), 0)
                """, REGISTRATION_ID, SEASON_ID, DIVISION_ID, TEAM_ID, captain, captainMember);
        int sort = 0;
        for (Long playerId : players) {
            Long memberId = jdbc.queryForObject(
                    "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
            jdbc.update("""
                    INSERT INTO basket_registration_player
                    (registration_id, division_id, season_id, team_id, player_id, member_id, platform_code,
                     is_captain, is_starter, sort_order, create_time, update_time, is_delete)
                    VALUES (?, ?, ?, ?, ?, ?, 'PS_XBOX', ?, 1, ?, NOW(), NOW(), 0)
                    """, REGISTRATION_ID, DIVISION_ID, SEASON_ID, TEAM_ID, playerId, memberId,
                    playerId.equals(captain) ? 1 : 0, sort++);
        }
    }

    private void insertMember(long playerId, boolean captain) {
        Long memberId = jdbc.queryForObject(
                "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
        jdbc.update("""
                INSERT INTO basket_team_member
                (membership_no, team_id, division_id, member_id, player_id, role_type, join_source, membership_status,
                 is_current, join_time, create_time, update_time, is_delete)
                VALUES (?, ?, ?, ?, ?, ?, 'CREATE', 1, 1, NOW(), NOW(), NOW(), 0)
                """, "E2E-WEB-" + playerId, TEAM_ID, DIVISION_ID, memberId, playerId, captain ? "CAPTAIN" : "MEMBER");
    }

    private void cleanup() {
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? OR match_no = 'E2E-WEB-M'", DIVISION_ID);
        jdbc.update("DELETE FROM basket_registration_audit WHERE registration_id = ?", REGISTRATION_ID);
        jdbc.update("DELETE FROM basket_registration_player WHERE registration_id = ?", REGISTRATION_ID);
        jdbc.update("DELETE FROM basket_registration WHERE id = ?", REGISTRATION_ID);
        jdbc.update("DELETE FROM basket_team_member WHERE team_id = ?", TEAM_ID);
        jdbc.update("DELETE FROM basket_team WHERE id = ?", TEAM_ID);
        jdbc.update("DELETE FROM basket_division WHERE id = ?", DIVISION_ID);
        jdbc.update("DELETE FROM basket_season WHERE id = ?", SEASON_ID);
    }

    private void copyRow(String table, long sourceId, long newId, Map<String, Object> overrides) {
        List<String> columns = jdbc.query("""
                SELECT COLUMN_NAME, EXTRA FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """, (rs, rowNum) -> {
            String extra = rs.getString("EXTRA") == null ? "" : rs.getString("EXTRA");
            if (extra.contains("VIRTUAL GENERATED") || extra.contains("STORED GENERATED") || extra.contains("auto_increment")) {
                return null;
            }
            return rs.getString("COLUMN_NAME");
        }, table);
        columns.removeIf(item -> item == null);
        String select = "SELECT " + String.join(",", columns.stream().map(col -> "`" + col + "`").toList())
                + " FROM `" + table + "` WHERE id = ?";
        Map<String, Object> values = new LinkedHashMap<>(jdbc.queryForMap(select, sourceId));
        values.put("id", newId);
        values.putAll(overrides);
        String names = String.join(",", values.keySet().stream().map(col -> "`" + col + "`").toList());
        String marks = String.join(",", values.keySet().stream().map(col -> "?").toList());
        jdbc.update("INSERT INTO `" + table + "` (" + names + ") VALUES (" + marks + ")", values.values().toArray());
    }

    private static void assertSafeLocalUrl(String url) {
        assertTrue(url.contains("/basketball"), url);
        String host = url.replaceFirst("^jdbc:mysql://", "").replaceFirst("[:/].*", "");
        boolean local = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || host.startsWith("192.168.")
                || host.startsWith("10.");
        assertTrue(local, url);
    }
}
