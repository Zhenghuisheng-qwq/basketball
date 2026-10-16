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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MiniApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RosterFreezeLocalTest {

    private static final long SEASON_ID = 991001L;
    private static final long DIVISION_ID = 991011L;
    private static final long TEAM_ID = 991021L;
    private static final long REGISTRATION_ID = 991031L;
    private static final long HISTORICAL_PLAYER = 2108815352947290114L;

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
    void assertLocal() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertSafeLocalUrl(connection.getMetaData().getURL());
            assertEquals("basketball", connection.getCatalog());
        }
    }

    @AfterAll
    void cleanupFixture() {
        if (jdbc != null) {
            cleanup();
        }
    }

    @Test
    void freezeRejoinAndHistoricalStaleRows() {
        cleanup();
        seed();
        try {
            long leaver = players.get(1);
            leave(leaver);
            assertEquals(1, deleted(leaver));
            assertEquals(2, rosterSync.countEffectiveRoster(REGISTRATION_ID, TEAM_ID));
            assertEquals(1, eventDeleted(leaver));

            rosterSync.retireStaleBeforeRejoin(TEAM_ID, leaver);
            reactivate(leaver);
            assertEquals(1, deleted(leaver));
            assertEquals(2, rosterSync.countEffectiveRoster(REGISTRATION_ID, TEAM_ID));

            long frozenPlayer = players.get(2);
            int lineup = lineupCount();
            markMatch("PLAYING");
            leave(frozenPlayer);
            assertEquals(0, deleted(frozenPlayer));
            assertEquals(0, eventDeleted(frozenPlayer));
            assertEquals(lineup, lineupCount());
            assertEquals(2, rosterSync.countEffectiveRoster(REGISTRATION_ID, TEAM_ID));

            jdbc.update("UPDATE basket_match SET match_status = 'FINISHED' WHERE division_id = ? AND match_no = 'FREEZE-M'", DIVISION_ID);
            assertEquals(0, rosterSync.releaseFromWritableRegistrations(TEAM_ID, players.get(0), "结束后不应释放"));
            assertEquals(0, deleted(players.get(0)));

            jdbc.update("UPDATE basket_match SET match_status = 'FORFEITED' WHERE division_id = ? AND match_no = 'FREEZE-M'", DIVISION_ID);
            assertEquals(0, rosterSync.releaseFromWritableRegistrations(TEAM_ID, players.get(0), "弃权后不应释放"));

            jdbc.update("DELETE FROM basket_match_player_stat WHERE division_id = ?", DIVISION_ID);
            jdbc.update("DELETE FROM basket_match WHERE division_id = ?", DIVISION_ID);
            jdbc.update("UPDATE basket_division SET division_status = 'FINISHED' WHERE id = ?", DIVISION_ID);
            assertEquals(0, rosterSync.releaseFromWritableRegistrations(TEAM_ID, players.get(0), "赛区结束不应释放"));
            assertEquals(0, deleted(players.get(0)));
            jdbc.update("UPDATE basket_division SET division_status = 'ARCHIVED' WHERE id = ?", DIVISION_ID);
            assertEquals(0, rosterSync.releaseFromWritableRegistrations(TEAM_ID, players.get(0), "归档不应释放"));
            assertEquals(0, deleted(players.get(0)));

            String status354 = jdbc.queryForObject(
                    "SELECT registration_status FROM basket_registration WHERE id = 354", String.class);
            String status362 = jdbc.queryForObject(
                    "SELECT registration_status FROM basket_registration WHERE id = 362", String.class);
            rosterSync.retireHistoricalStaleRows();
            assertEquals(status354, jdbc.queryForObject(
                    "SELECT registration_status FROM basket_registration WHERE id = 354", String.class));
            assertEquals(status362, jdbc.queryForObject(
                    "SELECT registration_status FROM basket_registration WHERE id = 362", String.class));
            assertEquals(1, jdbc.queryForObject("""
                    SELECT is_delete FROM basket_registration_player
                    WHERE registration_id = 354 AND player_id = ?
                    """, Integer.class, HISTORICAL_PLAYER));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT is_delete FROM basket_registration_player
                    WHERE registration_id = 362 AND player_id = ?
                    """, Integer.class, HISTORICAL_PLAYER));
            assertEquals(0, rosterSync.retireHistoricalStaleRows());
        } finally {
            UserContext.clear();
            cleanup();
        }
    }

    private void markMatch(String status) {
        Long stageId = jdbc.queryForObject("SELECT stage_id FROM basket_match LIMIT 1", Long.class);
        Long eventTeamId = jdbc.queryForObject(
                "SELECT id FROM basket_event_team WHERE division_id = ? AND source_team_id = ?",
                Long.class, DIVISION_ID, TEAM_ID);
        jdbc.update("""
                INSERT INTO basket_match
                (season_id, division_id, match_no, stage_id, match_status, publish_status, is_delete, create_time, update_time)
                VALUES (?, ?, 'FREEZE-M', ?, ?, 'DRAFT', 0, NOW(), NOW())
                """, SEASON_ID, DIVISION_ID, stageId, status);
        Long eventPlayerId = jdbc.queryForObject("""
                SELECT id FROM basket_event_player WHERE event_team_id = ? AND source_player_id = ?
                """, Long.class, eventTeamId, players.get(2));
        jdbc.update("""
                INSERT INTO basket_match_player_stat
                (match_id, season_id, division_id, event_team_id, event_player_id, points, is_delete, create_time, update_time)
                SELECT id, ?, ?, ?, ?, 3, 0, NOW(), NOW() FROM basket_match
                WHERE division_id = ? AND match_no = 'FREEZE-M'
                """, SEASON_ID, DIVISION_ID, eventTeamId, eventPlayerId, DIVISION_ID);
    }

    private void leave(long playerId) {
        Long memberId = jdbc.queryForObject(
                "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
        UserContext.setUserId(memberId);
        teamMemberService.leave(String.valueOf(TEAM_ID), "127.0.0.1");
        UserContext.clear();
    }

    private void reactivate(long playerId) {
        jdbc.update("""
                INSERT INTO basket_team_member
                (membership_no, team_id, division_id, member_id, player_id, role_type, join_source, membership_status,
                 is_current, join_time, create_time, update_time, is_delete)
                SELECT ?, ?, ?, member_id, id, 'MEMBER', 'CREATE', 1, 1, NOW(), NOW(), NOW(), 0
                FROM basket_player_profile WHERE id = ?
                """, "FZ-RE-" + playerId, TEAM_ID, DIVISION_ID, playerId);
    }

    private int deleted(long playerId) {
        Integer value = jdbc.queryForObject("""
                SELECT is_delete FROM basket_registration_player
                WHERE registration_id = ? AND player_id = ?
                """, Integer.class, REGISTRATION_ID, playerId);
        return value == null ? 1 : value;
    }

    private int eventDeleted(long playerId) {
        Integer value = jdbc.queryForObject("""
                SELECT is_delete FROM basket_event_player
                WHERE division_id = ? AND source_player_id = ?
                ORDER BY id DESC LIMIT 1
                """, Integer.class, DIVISION_ID, playerId);
        return value == null ? 1 : value;
    }

    private int lineupCount() {
        return jdbc.queryForObject(
                "SELECT lineup_count FROM basket_registration WHERE id = ?", Integer.class, REGISTRATION_ID);
    }

    private void seed() {
        copyRow("basket_season", 184L, SEASON_ID, Map.of(
                "season_year", 2095, "season_name", "FZ-S", "display_title", "Freeze",
                "publish_status", "DRAFT", "season_status", "NOT_STARTED"));
        copyRow("basket_division", 213L, DIVISION_ID, Map.of(
                "season_id", SEASON_ID, "division_name", "Freeze Division",
                "division_status", "REGISTRATION_OPEN", "platform_code", "FZ_PC"));
        copyRow("basket_team", 2108511641754677249L, TEAM_ID, Map.of(
                "team_no", "FZ-T", "team_name", "Freeze", "short_name", "FZ", "division_id", DIVISION_ID));
        players.clear();
        players.addAll(jdbc.query("""
                SELECT p.id FROM basket_player_profile p
                WHERE p.is_delete = 0 AND p.certification_status = 3
                  AND p.id NOT IN (SELECT player_id FROM basket_team_member WHERE is_current = 1 AND is_delete = 0)
                ORDER BY p.id LIMIT 3
                """, (rs, row) -> rs.getLong(1)));
        if (players.size() < 3) {
            throw new IllegalStateException("可用球员不足");
        }
        for (int i = 0; i < players.size(); i++) {
            Long memberId = jdbc.queryForObject(
                    "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, players.get(i));
            jdbc.update("""
                    INSERT INTO basket_team_member
                    (membership_no, team_id, division_id, member_id, player_id, role_type, join_source, membership_status,
                     is_current, join_time, create_time, update_time, is_delete)
                    VALUES (?, ?, ?, ?, ?, ?, 'CREATE', 1, 1, NOW(), NOW(), NOW(), 0)
                    """, "FZ-" + players.get(i), TEAM_ID, DIVISION_ID, memberId, players.get(i), i == 0 ? "CAPTAIN" : "MEMBER");
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
        jdbc.update("""
                INSERT INTO basket_event_team
                (season_id, division_id, source_team_id, registration_id, team_name_snapshot, event_team_status, is_delete, create_time, update_time)
                VALUES (?, ?, ?, ?, 'Freeze', 'ACTIVE', 0, NOW(), NOW())
                """, SEASON_ID, DIVISION_ID, TEAM_ID, REGISTRATION_ID);
        Long eventTeamId = jdbc.queryForObject(
                "SELECT id FROM basket_event_team WHERE division_id = ? AND source_team_id = ?",
                Long.class, DIVISION_ID, TEAM_ID);
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
            jdbc.update("""
                    INSERT INTO basket_event_player
                    (season_id, division_id, event_team_id, source_player_id, platform_code, event_player_status, is_delete, create_time, update_time)
                    VALUES (?, ?, ?, ?, 'PS_XBOX', 'ACTIVE', 0, NOW(), NOW())
                    """, SEASON_ID, DIVISION_ID, eventTeamId, playerId);
        }
    }

    private void cleanup() {
        jdbc.update("DELETE FROM basket_account_security_log WHERE new_value_masked = ? AND client_ip = '127.0.0.1'", String.valueOf(TEAM_ID));
        jdbc.update("DELETE FROM basket_match_player_stat WHERE division_id = ?", DIVISION_ID);
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? OR match_no = 'FREEZE-M'", DIVISION_ID);
        jdbc.update("DELETE FROM basket_event_player WHERE division_id = ?", DIVISION_ID);
        jdbc.update("DELETE FROM basket_event_team WHERE division_id = ?", DIVISION_ID);
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
        Map<String, Object> values = new LinkedHashMap<>(jdbc.queryForMap(
                "SELECT " + String.join(",", columns.stream().map(col -> "`" + col + "`").toList())
                        + " FROM `" + table + "` WHERE id = ?", sourceId));
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
