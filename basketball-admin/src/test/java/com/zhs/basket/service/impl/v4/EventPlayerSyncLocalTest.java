package com.zhs.basket.service.impl.v4;

import com.zhs.RuoYiApplication;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.support.v4.PreMatchEventPlayerSync;
import com.zhs.common.exception.ServiceException;
import com.zhs.system.model.LoginUser;
import com.zhs.system.model.SysUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = RuoYiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventPlayerSyncLocalTest {

    private static final long SEASON_ID = 991101L;
    private static final long DIVISION_ID = 991111L;
    private static final long TEAM_ID = 991121L;
    private static final long REGISTRATION_ID = 991131L;
    private static final long CHANGE_ID = 991141L;
    private static final long CHANGE_RACE = 991142L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private BasketV4RosterChangeAuditServiceImpl rosterChangeService;
    @Autowired
    private PreMatchEventPlayerSync eventPlayerSync;

    @BeforeAll
    void assertLocal() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertSafeLocalUrl(connection.getMetaData().getURL());
        }
        login();
    }

    @AfterAll
    void cleanupFixture() {
        if (jdbc != null) {
            cleanup();
        }
    }

    @Test
    void scheduledChangeSyncsEventPlayersAndFrozenChangeRollsBack() throws Exception {
        cleanup();
        List<Long> players = jdbc.query("""
                SELECT p.id FROM basket_player_profile p
                JOIN basket_player_platform_account a ON a.player_id = p.id AND a.is_delete = 0
                WHERE p.is_delete = 0 AND p.certification_status = 3 AND a.account_status = 1
                  AND a.platform IN ('PS', 'XBOX')
                GROUP BY p.id ORDER BY p.id LIMIT 6
                """, (rs, row) -> rs.getLong(1));
        assertTrue(players.size() >= 6);
        List<Long> original = new ArrayList<>(players.subList(0, 5));
        long removed = original.get(4);
        long added = players.get(5);
        long captain = original.get(0);
        try {
            copyRow("basket_season", 184L, SEASON_ID, Map.of(
                    "season_year", 2094, "season_name", "SYNC-S", "display_title", "Sync",
                    "publish_status", "DRAFT", "season_status", "NOT_STARTED"));
            copyRow("basket_division", 213L, DIVISION_ID, Map.of(
                    "season_id", SEASON_ID, "division_name", "Sync Division",
                    "division_status", "REGISTRATION_OPEN"));
            copyRow("basket_team", 2108511641754677249L, TEAM_ID, Map.of(
                    "team_no", "SYNC-T", "team_name", "Sync", "short_name", "SY", "division_id", DIVISION_ID));
            for (Long playerId : players) {
                insertMember(playerId, playerId == captain);
            }
            Long captainMember = jdbc.queryForObject(
                    "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, captain);
            jdbc.update("""
                    INSERT INTO basket_registration
                    (id, season_id, division_id, registration_mode, registration_source, team_id, captain_player_id,
                     captain_member_id, registration_status, lineup_count, lineup_version, submit_time, approve_time,
                     create_time, update_time, is_delete)
                    VALUES (?, ?, ?, 'TEAM', 'TEAM_DIRECT', ?, ?, ?, 'APPROVED', 5, 0, NOW(), NOW(), NOW(), NOW(), 0)
                    """, REGISTRATION_ID, SEASON_ID, DIVISION_ID, TEAM_ID, captain, captainMember);
            insertRoster(original, captain);
            jdbc.update("""
                    INSERT INTO basket_event_team
                    (season_id, division_id, source_team_id, registration_id, team_name_snapshot, event_team_status, is_delete, create_time, update_time)
                    VALUES (?, ?, ?, ?, 'Sync', 'ACTIVE', 0, NOW(), NOW())
                    """, SEASON_ID, DIVISION_ID, TEAM_ID, REGISTRATION_ID);
            Long eventTeamId = jdbc.queryForObject(
                    "SELECT id FROM basket_event_team WHERE division_id = ?", Long.class, DIVISION_ID);
            for (Long playerId : original) {
                jdbc.update("""
                        INSERT INTO basket_event_player
                        (season_id, division_id, event_team_id, source_player_id, platform_code, event_player_status, is_delete, create_time, update_time)
                        VALUES (?, ?, ?, ?, 'PS_XBOX', 'ACTIVE', 0, NOW(), NOW())
                        """, SEASON_ID, DIVISION_ID, eventTeamId, playerId);
            }
            Long stageId = jdbc.queryForObject("SELECT stage_id FROM basket_match LIMIT 1", Long.class);
            jdbc.update("""
                    INSERT INTO basket_match
                    (season_id, division_id, match_no, stage_id, home_event_team_id, match_status, publish_status, is_delete, create_time, update_time)
                    VALUES (?, ?, 'SYNC-M', ?, ?, 'SCHEDULED', 'DRAFT', 0, NOW(), NOW())
                    """, SEASON_ID, DIVISION_ID, stageId, eventTeamId);
            List<Long> target = new ArrayList<>(original.subList(0, 4));
            target.add(added);
            insertChange(CHANGE_ID, 0, target, captain);
            login();
            rosterChangeService.approve(CHANGE_ID);

            assertTrue(activeRegistrationPlayers().contains(added));
            assertFalse(activeRegistrationPlayers().contains(removed));
            assertTrue(activeEventPlayers(eventTeamId).contains(added));
            assertFalse(activeEventPlayers(eventTeamId).contains(removed));
            assertEquals(5, activeEventPlayers(eventTeamId).size());
            assertEquals("SCHEDULED", jdbc.queryForObject(
                    "SELECT match_status FROM basket_match WHERE match_no = 'SYNC-M'", String.class));

            BasketRegistration registration = new BasketRegistration();
            registration.setId(REGISTRATION_ID);
            registration.setDivisionId(DIVISION_ID);
            registration.setTeamId(TEAM_ID);
            registration.setSeasonId(SEASON_ID);
            eventPlayerSync.syncWritable(registration);
            assertEquals(5, activeEventPlayers(eventTeamId).size());
            assertEquals(5, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM basket_event_player WHERE event_team_id = ?",
                    Integer.class, eventTeamId).intValue() - jdbc.queryForObject(
                    "SELECT COUNT(*) FROM basket_event_player WHERE event_team_id = ? AND is_delete = 1",
                    Integer.class, eventTeamId));

            jdbc.update("UPDATE basket_match SET match_status = 'PLAYING' WHERE match_no = 'SYNC-M'");
            insertChange(CHANGE_RACE, 1, original, captain);
            ServiceException blocked = assertThrows(ServiceException.class, () -> rosterChangeService.approve(CHANGE_RACE));
            assertTrue(blocked.getMessage().contains("比赛已开始") || blocked.getMessage().contains("统计"));
            assertFalse(activeEventPlayers(eventTeamId).contains(removed));
            assertTrue(activeRegistrationPlayers().contains(added));

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger ok = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<?> left = pool.submit(() -> race(CHANGE_RACE, ready, start, ok, rejected));
            Future<?> right = pool.submit(() -> race(CHANGE_RACE, ready, start, ok, rejected));
            ready.await();
            start.countDown();
            left.get();
            right.get();
            pool.shutdown();
            assertEquals(0, ok.get());
            assertEquals(2, rejected.get());
        } finally {
            cleanup();
        }
    }

    private void race(long changeId, CountDownLatch ready, CountDownLatch start, AtomicInteger ok, AtomicInteger rejected) {
        login();
        ready.countDown();
        try {
            start.await();
            rosterChangeService.approve(changeId);
            ok.incrementAndGet();
        } catch (Exception ex) {
            rejected.incrementAndGet();
        }
    }

    private List<Long> activeRegistrationPlayers() {
        return jdbc.queryForList(
                "SELECT player_id FROM basket_registration_player WHERE registration_id = ? AND is_delete = 0",
                Long.class, REGISTRATION_ID);
    }

    private List<Long> activeEventPlayers(long eventTeamId) {
        return jdbc.queryForList(
                "SELECT source_player_id FROM basket_event_player WHERE event_team_id = ? AND is_delete = 0",
                Long.class, eventTeamId);
    }

    private void insertMember(long playerId, boolean captain) {
        Long memberId = jdbc.queryForObject(
                "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
        jdbc.update("""
                INSERT INTO basket_team_member
                (membership_no, team_id, division_id, member_id, player_id, role_type, join_source, membership_status,
                 is_current, join_time, create_time, update_time, is_delete)
                VALUES (?, ?, ?, ?, ?, ?, 'CREATE', 1, 1, NOW(), NOW(), NOW(), 0)
                """, "SYNC-" + playerId, TEAM_ID, DIVISION_ID, memberId, playerId, captain ? "CAPTAIN" : "MEMBER");
    }

    private void insertRoster(List<Long> players, long captainId) {
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
                    playerId == captainId ? 1 : 0, sort++);
        }
    }

    private void insertChange(long changeId, int version, List<Long> players, long captainId) {
        Long memberId = jdbc.queryForObject(
                "SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, captainId);
        jdbc.update("""
                INSERT INTO basket_registration_roster_change
                (id, registration_id, division_id, season_id, team_id, change_status, base_lineup_version,
                 submit_member_id, create_time, update_time, is_delete)
                VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?, NOW(), NOW(), 0)
                """, changeId, REGISTRATION_ID, DIVISION_ID, SEASON_ID, TEAM_ID, version, memberId);
        int sort = 0;
        for (Long playerId : players) {
            jdbc.update("""
                    INSERT INTO basket_registration_roster_change_player
                    (change_id, player_id, is_captain, is_starter, sort_order, create_time, is_delete)
                    VALUES (?, ?, ?, 1, ?, NOW(), 0)
                    """, changeId, playerId, playerId == captainId ? 1 : 0, sort++);
        }
    }

    private void cleanup() {
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? OR match_no = 'SYNC-M'", DIVISION_ID);
        jdbc.update("DELETE FROM basket_event_player WHERE division_id = ?", DIVISION_ID);
        jdbc.update("DELETE FROM basket_event_team WHERE division_id = ?", DIVISION_ID);
        jdbc.update("DELETE FROM basket_registration_roster_change_player WHERE change_id IN (?, ?)", CHANGE_ID, CHANGE_RACE);
        jdbc.update("DELETE FROM basket_registration_roster_change WHERE id IN (?, ?)", CHANGE_ID, CHANGE_RACE);
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

    private static void login() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("probe");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }
}
