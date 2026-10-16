package com.zhs.basket.service.impl.v4;

import com.zhs.RuoYiApplication;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.dto.v4.BasketCaptainGrantAdjustReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantSyncReq;
import com.zhs.basket.dto.v4.BasketDivisionUpdateReq;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.dto.v4.BasketSeasonUpdateReq;
import com.zhs.basket.dto.v4.TeamRosterAllocationReq;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.service.v4.IBasketV4CaptainGrantService;
import com.zhs.basket.service.v4.IBasketV4RegistrationAuditService;
import com.zhs.basket.service.v4.ITeamRosterAllocationService;
import com.zhs.basket.service.v4.IBasketV4DivisionService;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = RuoYiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalMysqlAcceptanceTest {

    private static final long SEASON_ID = 990184L;
    private static final long DIV_WITH_END = 990213L;
    private static final long DIV_SEASON_ONLY = 990214L;
    private static final long TEAM_A = 990301L;
    private static final long TEAM_B = 990302L;
    private static final long REG_A = 990501L;
    private static final long REG_B = 990502L;
    private static final long GRANT_AUTO = 990401L;
    private static final long GRANT_CUSTOM = 990402L;
    private static final long GRANT_LEGACY = 990403L;
    private static final long GRANT_REVOKED = 990404L;
    private static final long GRANT_EXPIRED = 990405L;
    private static final long GRANT_LAZY = 990406L;
    private static final long GRANT_SEASON = 990407L;
    private static final long GRANT_RACE = 990408L;
    private static final long CHANGE_A = 990601L;
    private static final long CHANGE_B = 990602L;
    private static final long CHANGE_REJECT = 990603L;
    private static final long CHANGE_STALE = 990604L;
    private static final long TEAM_C = 990311L;
    private static final long TEAM_D = 990312L;
    private static final long REG_C = 990511L;
    private static final long REG_D = 990512L;
    private static final long CHANGE_BLOCK = 990621L;
    private static final long CHANGE_TRANSFER = 990623L;
    private static final long CHANGE_RESTORE = 990624L;
    private static final long CHANGE_EXTRA = 990625L;
    private static final long PLAYER_P = 2108815352947290114L;
    private static final long CAPTAIN_A = 2108271004623056897L;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private IBasketV4CaptainGrantService grantService;
    @Autowired
    private IBasketV4DivisionService divisionService;
    @Autowired
    private IBasketV4SeasonService seasonService;
    @Autowired
    private BasketV4RosterChangeAuditServiceImpl rosterChangeService;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private IBasketV4RegistrationAuditService registrationAuditService;
    @Autowired
    private ITeamRosterAllocationService rosterAllocationService;

    private Long createdGrantId;
    private final List<String> failures = new ArrayList<>();

    @BeforeAll
    void assertLocalDatabaseAndSeed() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertSafeLocalUrl(connection.getMetaData().getURL());
            assertEquals("basketball", connection.getCatalog());
        }
        login();
        cleanup();
        seed();
    }

    @AfterAll
    void cleanupFixture() {
        login();
        cleanup();
    }

    @Test
    void localDatabaseAcceptance() throws Exception {
        login();
        a01NewGrantDefaultsToAutoEvent();
        a02DivisionEndSyncsOnlyAutoEvent();
        a03SeasonEndSyncsOnlySeasonDependentGrant();
        a04CustomIsNotOverwritten();
        a05RestoreAutoEvent();
        a06LegacyWasNotExtended();
        a07ExplicitSync();
        a08RevokedStaysRevoked();
        a09ExpiredStaysExpired();
        a10LazyExpiredActiveIsNotRevived();
        a11ConcurrentAdjustDoesNotLoseUpdate();
        a12PermissionUsesUpdatedValidTo();
        a13LogsExist();
        a14CannotPassEventEnd();
        bRosterChangeAndConcurrency();
        eligibilityFollowsCurrentMembers();
        assertHistoricalRowsUntouched();
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures));
        }
    }

    private void a01NewGrantDefaultsToAutoEvent() {
        Long playerId = jdbc.queryForObject("""
                SELECT p.id FROM basket_player_profile p
                JOIN basket_player_platform_account a ON a.player_id = p.id AND a.is_delete = 0
                WHERE p.is_delete = 0 AND p.certification_status = 3 AND a.account_status = 1
                  AND a.platform IN ('PS', 'XBOX')
                  AND NOT EXISTS (
                    SELECT 1 FROM basket_captain_grant g
                    WHERE g.division_id = ? AND g.player_id = p.id AND g.is_delete = 0 AND g.grant_status = 'ACTIVE'
                  )
                LIMIT 1
                """, Long.class, DIV_WITH_END);
        BasketCaptainGrantCreateReq req = new BasketCaptainGrantCreateReq();
        req.setDivisionId(DIV_WITH_END);
        req.setPlayerId(playerId);
        createdGrantId = grantService.create(req);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT valid_to_mode, valid_to FROM basket_captain_grant WHERE id = ?", createdGrantId);
        assertEquals("AUTO_EVENT", row.get("valid_to_mode"));
        assertSameTime("2026-11-20 00:00:00", row.get("valid_to"));
    }

    private void a02DivisionEndSyncsOnlyAutoEvent() {
        BasketDivisionUpdateReq req = new BasketDivisionUpdateReq();
        req.setMatchEndTime(Timestamp.valueOf("2026-11-25 00:00:00"));
        divisionService.update(DIV_WITH_END, req);
        assertSameTime("2026-11-25 00:00:00", validTo(GRANT_AUTO));
        assertSameTime("2026-11-10 00:00:00", validTo(GRANT_CUSTOM));
        assertSameTime("2026-10-18 00:00:00", validTo(GRANT_LEGACY));
        assertSameTime("2026-10-01 00:00:00", validTo(GRANT_LAZY));
    }

    private void a03SeasonEndSyncsOnlySeasonDependentGrant() {
        BasketSeasonUpdateReq req = new BasketSeasonUpdateReq();
        req.setSeasonEndTime(Timestamp.valueOf("2026-12-20 00:00:00"));
        seasonService.update(SEASON_ID, req);
        assertSameTime("2026-12-20 00:00:00", validTo(GRANT_SEASON));
        assertSameTime("2026-11-25 00:00:00", validTo(GRANT_AUTO));
    }

    private void a04CustomIsNotOverwritten() {
        BasketCaptainGrantAdjustReq adjust = new BasketCaptainGrantAdjustReq();
        adjust.setValidToMode("CUSTOM");
        adjust.setValidTo(Timestamp.valueOf("2026-11-12 00:00:00"));
        grantService.adjustValidTo(GRANT_CUSTOM, adjust);
        BasketDivisionUpdateReq req = new BasketDivisionUpdateReq();
        req.setMatchEndTime(Timestamp.valueOf("2026-11-28 00:00:00"));
        divisionService.update(DIV_WITH_END, req);
        assertSameTime("2026-11-12 00:00:00", validTo(GRANT_CUSTOM));
        assertEquals("CUSTOM", mode(GRANT_CUSTOM));
        assertSameTime("2026-11-28 00:00:00", validTo(GRANT_AUTO));
    }

    private void a05RestoreAutoEvent() {
        BasketCaptainGrantAdjustReq adjust = new BasketCaptainGrantAdjustReq();
        adjust.setValidToMode("AUTO_EVENT");
        grantService.adjustValidTo(GRANT_CUSTOM, adjust);
        assertEquals("AUTO_EVENT", mode(GRANT_CUSTOM));
        assertSameTime(validTo(GRANT_AUTO), validTo(GRANT_CUSTOM));
    }

    private void a06LegacyWasNotExtended() {
        assertSameTime("2026-10-18 00:00:00", validTo(GRANT_LEGACY));
        assertEquals(null, mode(GRANT_LEGACY));
    }

    private void a07ExplicitSync() {
        BasketCaptainGrantSyncReq req = new BasketCaptainGrantSyncReq();
        req.setIds(List.of(GRANT_LEGACY));
        var preview = grantService.previewSync(req);
        assertTrue(preview.getItems().get(0).isApplicable());
        grantService.syncEventTime(req);
        assertEquals("AUTO_EVENT", mode(GRANT_LEGACY));
        assertSameTime(validTo(GRANT_AUTO), validTo(GRANT_LEGACY));
    }

    private void a08RevokedStaysRevoked() {
        BasketCaptainGrantSyncReq req = new BasketCaptainGrantSyncReq();
        req.setIds(List.of(GRANT_REVOKED));
        var preview = grantService.previewSync(req);
        assertFalse(preview.getItems().get(0).isApplicable());
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT grant_status FROM basket_captain_grant WHERE id = ?", String.class, GRANT_REVOKED));
    }

    private void a09ExpiredStaysExpired() {
        BasketCaptainGrantSyncReq req = new BasketCaptainGrantSyncReq();
        req.setIds(List.of(GRANT_EXPIRED));
        assertFalse(grantService.previewSync(req).getItems().get(0).isApplicable());
        assertEquals("EXPIRED", jdbc.queryForObject(
                "SELECT grant_status FROM basket_captain_grant WHERE id = ?", String.class, GRANT_EXPIRED));
    }

    private void a10LazyExpiredActiveIsNotRevived() {
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT grant_status FROM basket_captain_grant WHERE id = ?", String.class, GRANT_LAZY));
        assertSameTime("2026-10-01 00:00:00", validTo(GRANT_LAZY));
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setValidTo(Timestamp.valueOf("2026-10-01 00:00:00"));
        grant.setGrantStatus("ACTIVE");
        assertFalse(participationSupport.isGrantUsable(reload(GRANT_LAZY)));
    }

    private void a11ConcurrentAdjustDoesNotLoseUpdate() throws Exception {
        Date left = Timestamp.valueOf("2026-11-18 00:00:00");
        Date right = Timestamp.valueOf("2026-11-19 00:00:00");
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> one = pool.submit(() -> adjustRace(GRANT_RACE, left, ready, start, success, conflict));
        Future<?> two = pool.submit(() -> adjustRace(GRANT_RACE, right, ready, start, success, conflict));
        ready.await();
        start.countDown();
        one.get();
        two.get();
        pool.shutdown();
        assertEquals(1, success.get());
        assertEquals(1, conflict.get());
        long saved = validMillis(validTo(GRANT_RACE));
        assertTrue(saved == Timestamp.valueOf("2026-11-18 00:00:00").getTime()
                || saved == Timestamp.valueOf("2026-11-19 00:00:00").getTime());
        assertEquals("CUSTOM", mode(GRANT_RACE));
    }

    private void a12PermissionUsesUpdatedValidTo() {
        BasketCaptainGrant future = reload(GRANT_RACE);
        assertTrue(participationSupport.isGrantUsable(future));
        jdbc.update("UPDATE basket_captain_grant SET valid_to = '2026-10-01 00:00:00' WHERE id = ?", GRANT_RACE);
        assertFalse(participationSupport.isGrantUsable(reload(GRANT_RACE)));
        jdbc.update("UPDATE basket_captain_grant SET valid_to = '2026-11-18 00:00:00' WHERE id = ?", GRANT_RACE);
    }

    private void a13LogsExist() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM basket_captain_grant_log
                WHERE grant_id IN (?,?,?) AND action_type IN ('ADJUST_VALID_TO','SYNC_EVENT_TIME') AND is_delete = 0
                """, Integer.class, GRANT_CUSTOM, GRANT_LEGACY, GRANT_RACE);
        assertTrue(count != null && count >= 3);
    }

    private void a14CannotPassEventEnd() {
        BasketCaptainGrantAdjustReq req = new BasketCaptainGrantAdjustReq();
        req.setValidToMode("CUSTOM");
        req.setValidTo(Timestamp.valueOf("2027-01-01 00:00:00"));
        ServiceException ex = assertThrows(ServiceException.class, () -> grantService.adjustValidTo(GRANT_AUTO, req));
        assertEquals("授权截止时间不能晚于赛事结束时间", ex.getMessage());
    }

    private void bRosterChangeAndConcurrency() throws Exception {
        assertTrue(playerIds(REG_A).contains(PLAYER_P));
        List<Long> rejectBefore = playerIds(REG_B);
        BasketRegistrationAuditRejectReq reject = new BasketRegistrationAuditRejectReq();
        reject.setRejectReason("人数调整驳回");
        rosterChangeService.reject(CHANGE_REJECT, reject);
        assertEquals(rejectBefore, playerIds(REG_B));
        assertEquals("APPROVED", statusOf(REG_B));

        ServiceException stale = assertThrows(ServiceException.class, () -> rosterChangeService.approve(CHANGE_STALE));
        assertTrue(stale.getMessage().contains("原名单已变化"));

        Long stageId = jdbc.queryForObject("SELECT stage_id FROM basket_match LIMIT 1", Long.class);
        jdbc.update("""
                INSERT INTO basket_match (season_id, division_id, match_no, stage_id, match_status, publish_status, is_delete, create_time, update_time)
                VALUES (?, ?, 'E2E-M', ?, 'PLAYING', 'DRAFT', 0, NOW(), NOW())
                """, SEASON_ID, DIV_WITH_END, stageId);
        ServiceException matched = assertThrows(ServiceException.class, () -> rosterChangeService.approve(CHANGE_B));
        assertTrue(matched.getMessage().contains("比赛已开始"));
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? AND match_no = 'E2E-M'", DIV_WITH_END);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> left = pool.submit(() -> approveRace(CHANGE_A, ready, start, ok, rejected));
        Future<?> right = pool.submit(() -> approveRace(CHANGE_B, ready, start, ok, rejected));
        ready.await();
        start.countDown();
        left.get();
        right.get();
        pool.shutdown();
        if (!"APPROVED".equals(changeStatus(CHANGE_A))) {
            rosterChangeService.approve(CHANGE_A);
        }
        if ("PENDING".equals(changeStatus(CHANGE_B))) {
            rosterChangeService.approve(CHANGE_B);
        }
        assertFalse(playerIds(REG_A).contains(PLAYER_P));
        assertTrue(playerIds(REG_B).contains(PLAYER_P));
        assertEquals("APPROVED", statusOf(REG_A));
        assertEquals("APPROVED", statusOf(REG_B));
        int holders = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT r.id) FROM basket_registration_player rp
                JOIN basket_registration r ON r.id = rp.registration_id AND r.is_delete = 0
                WHERE rp.player_id = ? AND rp.division_id = ? AND rp.is_delete = 0
                  AND r.registration_status = 'APPROVED'
                """, Integer.class, PLAYER_P, DIV_WITH_END);
        assertEquals(1, holders);
        ServiceException second = assertThrows(ServiceException.class, () -> rosterChangeService.approve(CHANGE_A));
        assertTrue(second.getMessage().contains("已处理"));
    }

    private void eligibilityFollowsCurrentMembers() {
        List<Long> pool = jdbc.query("""
                SELECT p.id FROM basket_player_profile p
                JOIN basket_player_platform_account a ON a.player_id = p.id AND a.is_delete = 0
                WHERE p.is_delete = 0 AND p.certification_status = 3 AND a.account_status = 1
                  AND a.platform IN ('PS', 'XBOX')
                  AND p.id NOT IN (SELECT player_id FROM basket_team_member WHERE team_id IN (?, ?) AND is_delete = 0)
                GROUP BY p.id
                ORDER BY p.id
                LIMIT 13
                """, (rs, row) -> rs.getLong(1), TEAM_A, TEAM_B);
        if (pool.size() < 13) {
            throw new IllegalStateException("测试库可用球员不足: " + pool.size());
        }
        List<Long> squadC = new ArrayList<>(pool.subList(0, 8));
        List<Long> squadD = new ArrayList<>(pool.subList(8, 12));
        long restorePlayer = pool.get(12);
        long captainC = squadC.get(0);
        long captainD = squadD.get(0);
        long moving = squadC.get(4);
        copyRow("basket_team", 2108511641754677249L, TEAM_C, Map.of(
                "team_no", "E2E-TC", "team_name", "E2E C", "short_name", "EC", "division_id", DIV_WITH_END));
        copyRow("basket_team", 2108511641754677249L, TEAM_D, Map.of(
                "team_no", "E2E-TD", "team_name", "E2E D", "short_name", "ED", "division_id", DIV_WITH_END));
        for (Long playerId : squadC) {
            insertMember(TEAM_C, playerId, playerId == captainC);
        }
        for (Long playerId : squadD) {
            insertMember(TEAM_D, playerId, playerId == captainD);
        }
        insertRegistration(REG_C, TEAM_C, captainC, "APPROVED");
        insertRegistration(REG_D, TEAM_D, captainD, "APPROVED");
        insertRoster(REG_C, TEAM_C, squadC, captainC);
        insertRoster(REG_D, TEAM_D, squadD, captainD);
        jdbc.update("UPDATE basket_registration SET lineup_count = 8 WHERE id = ?", REG_C);
        jdbc.update("UPDATE basket_registration SET lineup_count = 4 WHERE id = ?", REG_D);
        assertEquals(8, registrationPlayerMapper.countCurrentEligible(REG_C));
        assertEquals("ELIGIBLE", eligibilityOf(REG_C));

        jdbc.update("""
                UPDATE basket_team_member
                SET membership_status = 0, is_current = 0, leave_time = NOW(), leave_reason = 'E2E离队'
                WHERE team_id = ? AND player_id IN (?,?,?,?)
                """, TEAM_C, squadC.get(4), squadC.get(5), squadC.get(6), squadC.get(7));
        assertEquals(4, registrationPlayerMapper.countCurrentEligible(REG_C));
        assertEquals("INELIGIBLE", eligibilityOf(REG_C));
        TeamRosterAllocationReq assign = new TeamRosterAllocationReq();
        assign.setRegistrationId(REG_C);
        assign.setParticipationRole("FORMAL");
        ServiceException blocked = assertThrows(ServiceException.class,
                () -> rosterAllocationService.assign(DIV_WITH_END, assign, 1L));
        assertTrue(blocked.getMessage().contains("人数不足"));

        List<Long> still = squadC.subList(0, 4);
        insertChange(CHANGE_BLOCK, REG_C, TEAM_C, 0, still, captainC);
        Long stageId = jdbc.queryForObject("SELECT stage_id FROM basket_match LIMIT 1", Long.class);
        jdbc.update("""
                INSERT INTO basket_match (season_id, division_id, match_no, stage_id, match_status, publish_status, is_delete, create_time, update_time)
                VALUES (?, ?, 'E2E-M2', ?, 'PLAYING', 'DRAFT', 0, NOW(), NOW())
                """, SEASON_ID, DIV_WITH_END, stageId);
        ServiceException started = assertThrows(ServiceException.class, () -> rosterChangeService.approve(CHANGE_BLOCK));
        assertTrue(started.getMessage().contains("比赛已开始"));
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? AND match_no = 'E2E-M2'", DIV_WITH_END);
        BasketRegistrationAuditRejectReq reject = new BasketRegistrationAuditRejectReq();
        reject.setRejectReason("开赛拦截后关闭申请");
        rosterChangeService.reject(CHANGE_BLOCK, reject);

        jdbc.update("""
                UPDATE basket_registration_player
                SET is_delete = 1, update_time = NOW()
                WHERE registration_id = ? AND player_id IN (?,?,?,?)
                """, REG_C, squadC.get(4), squadC.get(5), squadC.get(6), squadC.get(7));
        jdbc.update("UPDATE basket_registration SET lineup_count = 4, lineup_version = lineup_version + 1 WHERE id = ?", REG_C);
        assertFalse(playerIds(REG_C).contains(moving));

        insertMember(TEAM_D, moving, false);
        List<Long> targetD = new ArrayList<>(squadD);
        targetD.add(moving);
        insertChange(CHANGE_TRANSFER, REG_D, TEAM_D, 0, targetD, captainD);
        rosterChangeService.approve(CHANGE_TRANSFER);
        assertTrue(playerIds(REG_D).contains(moving));
        assertFalse(playerIds(REG_C).contains(moving));
        Integer holders = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT r.id) FROM basket_registration_player rp
                JOIN basket_registration r ON r.id = rp.registration_id AND r.is_delete = 0
                WHERE rp.player_id = ? AND rp.division_id = ? AND rp.is_delete = 0
                  AND r.registration_status IN ('PENDING_REVIEW', 'APPROVED')
                """, Integer.class, moving, DIV_WITH_END);
        assertEquals(1, holders);

        insertMember(TEAM_C, restorePlayer, false);
        List<Long> restored = new ArrayList<>(still);
        restored.add(restorePlayer);
        int version = jdbc.queryForObject("SELECT lineup_version FROM basket_registration WHERE id = ?", Integer.class, REG_C);
        insertChange(CHANGE_RESTORE, REG_C, TEAM_C, version, restored, captainC);
        rosterChangeService.approve(CHANGE_RESTORE);
        assertEquals(5, registrationPlayerMapper.countCurrentEligible(REG_C));
        assertEquals("ELIGIBLE", eligibilityOf(REG_C));
        assertTrue(playerIds(REG_C).contains(restorePlayer));

        List<Long> extra = new ArrayList<>(restored);
        extra.add(squadC.get(5));
        int after = jdbc.queryForObject("SELECT lineup_version FROM basket_registration WHERE id = ?", Integer.class, REG_C);
        insertChange(CHANGE_EXTRA, REG_C, TEAM_C, after, extra, captainC);
        BasketRegistrationAuditRejectReq extraReject = new BasketRegistrationAuditRejectReq();
        extraReject.setRejectReason("不采纳新增");
        rosterChangeService.reject(CHANGE_EXTRA, extraReject);
        assertFalse(playerIds(REG_C).contains(squadC.get(5)));
        assertEquals(5, registrationPlayerMapper.countCurrentEligible(REG_C));
    }

    private String eligibilityOf(long registrationId) {
        BasketRegistrationAuditQuery query = new BasketRegistrationAuditQuery();
        query.setDivisionId(DIV_WITH_END);
        return registrationAuditService.selectList(query).stream()
                .filter(row -> registrationId == row.getRegistrationId())
                .findFirst()
                .orElseThrow()
                .getEligibilityStatus();
    }

    private void assertHistoricalRowsUntouched() {
        Integer grants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM basket_captain_grant WHERE division_id = 213 AND is_delete = 0 AND valid_to = '2026-10-18 00:00:00'",
                Integer.class);
        assertEquals(16, grants);
        assertEquals("APPROVED", jdbc.queryForObject(
                "SELECT registration_status FROM basket_registration WHERE id = 354", String.class));
        assertEquals("PENDING_REVIEW", jdbc.queryForObject(
                "SELECT registration_status FROM basket_registration WHERE id = 362", String.class));
    }

    private void seed() {
        copyRow("basket_season", 184L, SEASON_ID, Map.of(
                "season_year", 2099,
                "season_name", "E2E-S",
                "display_title", "E2E Season",
                "match_start_time", Timestamp.valueOf("2026-10-01 00:00:00"),
                "match_end_time", Timestamp.valueOf("2026-12-31 00:00:00"),
                "publish_status", "DRAFT",
                "season_status", "NOT_STARTED"));
        copyRow("basket_division", 213L, DIV_WITH_END, Map.of(
                "season_id", SEASON_ID,
                "division_name", "E2E Division",
                "division_status", "REGISTRATION_OPEN",
                "registration_start_time", Timestamp.valueOf("2026-10-01 00:00:00"),
                "registration_end_time", Timestamp.valueOf("2026-11-01 00:00:00"),
                "match_start_time", Timestamp.valueOf("2026-11-10 00:00:00"),
                "match_end_time", Timestamp.valueOf("2026-11-20 00:00:00")));
        Map<String, Object> seasonOnly = new LinkedHashMap<>();
        seasonOnly.put("season_id", SEASON_ID);
        seasonOnly.put("platform_code", "E2E_PC");
        seasonOnly.put("division_name", "E2E Season Only");
        seasonOnly.put("division_status", "REGISTRATION_OPEN");
        seasonOnly.put("match_end_time", null);
        copyRow("basket_division", 213L, DIV_SEASON_ONLY, seasonOnly);
        insertGrant(GRANT_AUTO, DIV_WITH_END, CAPTAIN_A, "AUTO_EVENT", "2026-11-20 00:00:00", "ACTIVE");
        insertGrant(GRANT_CUSTOM, DIV_WITH_END, 2108513243727147009L, "CUSTOM", "2026-11-10 00:00:00", "ACTIVE");
        insertGrant(GRANT_LEGACY, DIV_WITH_END, 2108516921540358145L, null, "2026-10-18 00:00:00", "ACTIVE");
        insertGrant(GRANT_REVOKED, DIV_WITH_END, 2108518011677716482L, "AUTO_EVENT", "2026-11-20 00:00:00", "REVOKED");
        insertGrant(GRANT_EXPIRED, DIV_WITH_END, 2108518751351615490L, "AUTO_EVENT", "2026-10-01 00:00:00", "EXPIRED");
        insertGrant(GRANT_LAZY, DIV_WITH_END, 2108654198933434370L, "AUTO_EVENT", "2026-10-01 00:00:00", "ACTIVE");
        insertGrant(GRANT_SEASON, DIV_SEASON_ONLY, CAPTAIN_A, "AUTO_EVENT", "2026-12-31 00:00:00", "ACTIVE");
        insertGrant(GRANT_RACE, DIV_WITH_END, PLAYER_P, "AUTO_EVENT", "2026-11-20 00:00:00", "ACTIVE");

        copyRow("basket_team", 2108511641754677249L, TEAM_A, Map.of(
                "team_no", "E2E-TA", "team_name", "E2E A", "short_name", "EA", "division_id", DIV_WITH_END));
        copyRow("basket_team", 2108936199821340674L, TEAM_B, Map.of(
                "team_no", "E2E-TB", "team_name", "E2E B", "short_name", "EB", "division_id", DIV_WITH_END));
        List<Long> squadA = List.of(CAPTAIN_A, 2108513243727147009L, 2108516921540358145L,
                2108518011677716482L, 2108518751351615490L, 2108654198933434370L);
        for (long playerId : squadA) {
            insertMember(TEAM_A, playerId, playerId == CAPTAIN_A);
        }
        List<Long> squadB = jdbc.query("""
                SELECT p.id FROM basket_player_profile p
                JOIN basket_player_platform_account a ON a.player_id = p.id AND a.is_delete = 0
                WHERE p.is_delete = 0 AND p.certification_status = 3 AND a.account_status = 1
                  AND a.platform IN ('PS', 'XBOX') AND p.id NOT IN (?,?,?,?,?,?,?)
                GROUP BY p.id LIMIT 4
                """, (rs, row) -> rs.getLong(1), CAPTAIN_A, 2108513243727147009L, 2108516921540358145L,
                2108518011677716482L, 2108518751351615490L, 2108654198933434370L, PLAYER_P);
        if (squadB.size() < 4) {
            throw new IllegalStateException("测试库缺少可用球员");
        }
        long captainB = squadB.get(0);
        insertGrant(990409L, DIV_WITH_END, captainB, "AUTO_EVENT", "2026-11-28 00:00:00", "ACTIVE");
        for (long playerId : squadB) {
            insertMember(TEAM_B, playerId, playerId == captainB);
        }
        insertMember(TEAM_B, PLAYER_P, false);
        insertRegistration(REG_A, TEAM_A, CAPTAIN_A, "APPROVED");
        insertRegistration(REG_B, TEAM_B, captainB, "APPROVED");
        insertRoster(REG_A, TEAM_A, new ArrayList<>(squadA) {{ add(PLAYER_P); }}, CAPTAIN_A);
        insertRoster(REG_B, TEAM_B, squadB, captainB);
        List<Long> targetA = squadA.subList(0, 5);
        insertChange(CHANGE_A, REG_A, TEAM_A, 0, targetA, CAPTAIN_A);
        List<Long> targetB = new ArrayList<>(squadB);
        targetB.add(PLAYER_P);
        insertChange(CHANGE_B, REG_B, TEAM_B, 0, targetB, captainB);
        insertChange(CHANGE_REJECT, REG_B, TEAM_B, 0, targetB, captainB);
        insertChange(CHANGE_STALE, REG_A, TEAM_A, 99, targetA, CAPTAIN_A);
    }

    private void insertGrant(long id, long divisionId, long playerId, String mode, String validTo, String status) {
        Long memberId = jdbc.queryForObject("SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
        String key = "ACTIVE".equals(status) ? divisionId + "_" + playerId : null;
        jdbc.update("""
                INSERT INTO basket_captain_grant
                (id, division_id, season_id, team_id, player_id, member_id, grant_status, active_unique_key,
                 valid_from, valid_to, valid_to_mode, grant_source, create_time, update_time, is_delete)
                VALUES (?, ?, ?, NULL, ?, ?, ?, ?, '2026-10-01 00:00:00', ?, ?, 'MANUAL', NOW(), NOW(), 0)
                """, id, divisionId, SEASON_ID, playerId, memberId, status, key, Timestamp.valueOf(validTo), mode);
    }

    private void insertMember(long teamId, long playerId, boolean captain) {
        Long memberId = jdbc.queryForObject("SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
        jdbc.update("""
                INSERT INTO basket_team_member
                (membership_no, team_id, division_id, member_id, player_id, role_type, join_source, membership_status,
                 is_current, join_time, create_time, update_time, is_delete)
                VALUES (?, ?, ?, ?, ?, ?, 'CREATE', 1, 1, NOW(), NOW(), NOW(), 0)
                """, "E2E-M-" + teamId + "-" + playerId, teamId, DIV_WITH_END, memberId, playerId, captain ? "CAPTAIN" : "MEMBER");
    }

    private void insertRegistration(long id, long teamId, long captainId, String status) {
        Long memberId = jdbc.queryForObject("SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, captainId);
        jdbc.update("""
                INSERT INTO basket_registration
                (id, season_id, division_id, registration_mode, registration_source, team_id, captain_player_id,
                 captain_member_id, registration_status, lineup_count, lineup_version, submit_time, approve_time,
                 create_time, update_time, is_delete)
                VALUES (?, ?, ?, 'TEAM', 'TEAM_DIRECT', ?, ?, ?, ?, 5, 0, NOW(), NOW(), NOW(), NOW(), 0)
                """, id, SEASON_ID, DIV_WITH_END, teamId, captainId, memberId, status);
    }

    private void insertRoster(long registrationId, long teamId, List<Long> players, long captainId) {
        int sort = 0;
        for (Long playerId : players) {
            Long memberId = jdbc.queryForObject("SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, playerId);
            jdbc.update("""
                    INSERT INTO basket_registration_player
                    (registration_id, division_id, season_id, team_id, player_id, member_id, platform_code,
                     is_captain, is_starter, sort_order, create_time, update_time, is_delete)
                    VALUES (?, ?, ?, ?, ?, ?, 'PS_XBOX', ?, 1, ?, NOW(), NOW(), 0)
                    """, registrationId, DIV_WITH_END, SEASON_ID, teamId, playerId, memberId,
                    playerId == captainId ? 1 : 0, sort++);
        }
    }

    private void insertChange(long changeId, long registrationId, long teamId, int version, List<Long> players, long captainId) {
        Long memberId = jdbc.queryForObject("SELECT member_id FROM basket_player_profile WHERE id = ?", Long.class, captainId);
        jdbc.update("""
                INSERT INTO basket_registration_roster_change
                (id, registration_id, division_id, season_id, team_id, change_status, base_lineup_version,
                 submit_member_id, create_time, update_time, is_delete)
                VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?, NOW(), NOW(), 0)
                """, changeId, registrationId, DIV_WITH_END, SEASON_ID, teamId, version, memberId);
        int sort = 0;
        for (Long playerId : players) {
            jdbc.update("""
                    INSERT INTO basket_registration_roster_change_player
                    (change_id, player_id, is_captain, is_starter, sort_order, create_time, is_delete)
                    VALUES (?, ?, ?, 1, ?, NOW(), 0)
                    """, changeId, playerId, playerId == captainId ? 1 : 0, sort++);
        }
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

    private void cleanup() {
        jdbc.update("DELETE FROM basket_match WHERE division_id IN (?, ?) OR match_no = 'E2E-M'", DIV_WITH_END, DIV_SEASON_ONLY);
        jdbc.update("DELETE FROM basket_match WHERE division_id = ? AND match_no = 'E2E-M2'", DIV_WITH_END);
        jdbc.update("DELETE FROM basket_registration_roster_change_player WHERE change_id IN (?,?,?,?,?,?,?,?)",
                CHANGE_A, CHANGE_B, CHANGE_REJECT, CHANGE_STALE, CHANGE_BLOCK, CHANGE_TRANSFER, CHANGE_RESTORE, CHANGE_EXTRA);
        jdbc.update("DELETE FROM basket_registration_roster_change WHERE id IN (?,?,?,?,?,?,?,?)",
                CHANGE_A, CHANGE_B, CHANGE_REJECT, CHANGE_STALE, CHANGE_BLOCK, CHANGE_TRANSFER, CHANGE_RESTORE, CHANGE_EXTRA);
        jdbc.update("DELETE FROM basket_registration_player WHERE registration_id IN (?, ?, ?, ?)", REG_A, REG_B, REG_C, REG_D);
        jdbc.update("DELETE FROM basket_registration_audit WHERE registration_id IN (?, ?, ?, ?)", REG_A, REG_B, REG_C, REG_D);
        jdbc.update("DELETE FROM basket_registration WHERE id IN (?, ?, ?, ?)", REG_A, REG_B, REG_C, REG_D);
        jdbc.update("DELETE FROM basket_team_member WHERE team_id IN (?, ?, ?, ?)", TEAM_A, TEAM_B, TEAM_C, TEAM_D);
        jdbc.update("DELETE FROM basket_team WHERE id IN (?, ?, ?, ?)", TEAM_A, TEAM_B, TEAM_C, TEAM_D);
        jdbc.update("DELETE FROM basket_captain_grant_log WHERE season_id = ? OR grant_id IN (?,?,?,?,?,?,?,?) OR grant_id = ?",
                SEASON_ID, GRANT_AUTO, GRANT_CUSTOM, GRANT_LEGACY, GRANT_REVOKED, GRANT_EXPIRED, GRANT_LAZY, GRANT_SEASON, GRANT_RACE,
                createdGrantId == null ? 0 : createdGrantId);
        jdbc.update("DELETE FROM basket_captain_grant WHERE id IN (?,?,?,?,?,?,?,?) OR (division_id IN (?, ?) AND id <> 0)",
                GRANT_AUTO, GRANT_CUSTOM, GRANT_LEGACY, GRANT_REVOKED, GRANT_EXPIRED, GRANT_LAZY, GRANT_SEASON, GRANT_RACE,
                DIV_WITH_END, DIV_SEASON_ONLY);
        if (createdGrantId != null) {
            jdbc.update("DELETE FROM basket_captain_grant_log WHERE grant_id = ?", createdGrantId);
            jdbc.update("DELETE FROM basket_captain_grant WHERE id = ?", createdGrantId);
        }
        jdbc.update("DELETE FROM basket_division WHERE id IN (?, ?)", DIV_WITH_END, DIV_SEASON_ONLY);
        jdbc.update("DELETE FROM basket_season WHERE id = ?", SEASON_ID);
    }

    private void adjustRace(long grantId, Date validTo, CountDownLatch ready, CountDownLatch start,
                            AtomicInteger success, AtomicInteger conflict) {
        login();
        ready.countDown();
        try {
            start.await();
            BasketCaptainGrantAdjustReq req = new BasketCaptainGrantAdjustReq();
            req.setValidToMode("CUSTOM");
            req.setValidTo(validTo);
            grantService.adjustValidTo(grantId, req);
            success.incrementAndGet();
        } catch (ServiceException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("已变化")) {
                conflict.incrementAndGet();
            } else {
                failures.add("adjust race: " + ex.getMessage());
            }
        } catch (Exception ex) {
            failures.add("adjust race: " + ex);
        }
    }

    private void approveRace(long changeId, CountDownLatch ready, CountDownLatch start,
                             AtomicInteger success, AtomicInteger rejected) {
        login();
        ready.countDown();
        try {
            start.await();
            rosterChangeService.approve(changeId);
            success.incrementAndGet();
        } catch (ServiceException ex) {
            rejected.incrementAndGet();
        } catch (Exception ex) {
            failures.add("approve race: " + ex);
        }
    }

    private Timestamp validTo(long id) {
        Object value = jdbc.queryForObject("SELECT valid_to FROM basket_captain_grant WHERE id = ?", Object.class, id);
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time);
        }
        return Timestamp.valueOf(String.valueOf(value).replace('T', ' '));
    }

    private void assertSameTime(String expected, Object actual) {
        assertSameTime(Timestamp.valueOf(expected), actual);
    }

    private void assertSameTime(Object expected, Object actual) {
        assertEquals(validMillis(expected), validMillis(actual));
    }

    private long validMillis(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.getTime();
        }
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time).getTime();
        }
        if (value instanceof Date date) {
            return date.getTime();
        }
        return Timestamp.valueOf(String.valueOf(value).replace('T', ' ')).getTime();
    }

    private String mode(long id) {
        return jdbc.queryForObject("SELECT valid_to_mode FROM basket_captain_grant WHERE id = ?", String.class, id);
    }

    private BasketCaptainGrant reload(long id) {
        return jdbc.queryForObject("SELECT id, grant_status, valid_from, valid_to, active_unique_key, division_id, player_id FROM basket_captain_grant WHERE id = ?",
                (rs, rowNum) -> {
                    BasketCaptainGrant grant = new BasketCaptainGrant();
                    grant.setId(rs.getLong("id"));
                    grant.setGrantStatus(rs.getString("grant_status"));
                    grant.setValidFrom(rs.getTimestamp("valid_from"));
                    grant.setValidTo(rs.getTimestamp("valid_to"));
                    grant.setActiveUniqueKey(rs.getString("active_unique_key"));
                    grant.setDivisionId(rs.getLong("division_id"));
                    grant.setPlayerId(rs.getLong("player_id"));
                    return grant;
                }, id);
    }

    private List<Long> playerIds(long registrationId) {
        return jdbc.queryForList(
                "SELECT player_id FROM basket_registration_player WHERE registration_id = ? AND is_delete = 0 ORDER BY sort_order, id",
                Long.class, registrationId);
    }

    private String changeStatus(long changeId) {
        return jdbc.queryForObject("SELECT change_status FROM basket_registration_roster_change WHERE id = ?", String.class, changeId);
    }

    private String statusOf(long registrationId) {
        return jdbc.queryForObject("SELECT registration_status FROM basket_registration WHERE id = ?", String.class, registrationId);
    }

    private int versionOf(long registrationId) {
        Integer version = jdbc.queryForObject("SELECT lineup_version FROM basket_registration WHERE id = ?", Integer.class, registrationId);
        return version == null ? 0 : version;
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
        user.setUserName("e2e");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }
}
