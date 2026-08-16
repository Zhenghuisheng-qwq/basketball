package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.req.DisciplineCreateReq;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.DisciplinePenaltyTargetCompatibilityPolicy;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketDisciplineServiceCompatibilityTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private BasketSeasonMapper seasonMapper;
    @Mock private BasketBusinessAuditWriter auditWriter;

    @InjectMocks
    private BasketDisciplineServiceImpl service;

    @Test
    void createRejectsPlayerEventDisqualificationEvenWithEventTeamId() {
        DisciplineCreateReq req = baseReq("PLAYER", "EVENT_DISQUALIFICATION");
        req.setPlayerId(1001L);
        req.setEventTeamId(231L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.create(req));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
        verify(disciplineRecordMapper, never()).insert(any());
    }

    @ParameterizedTest
    @CsvSource({
            "PERMANENT_TEAM,EVENT_DISQUALIFICATION,2001,",
            "ACCOUNT,EVENT_DISQUALIFICATION,,3001",
            "EVENT_TEAM,MATCH_SUSPENSION,2001,",
            "PLAYER,POINT_DEDUCTION,2001,",
            "EVENT_TEAM,CAPTAIN_BAN,2001,",
            "PLAYER,TEAM_SUSPENSION,2001,",
            "PLAYER,MATCH_ELIGIBILITY_CANCEL,2001,",
            "PLAYER,PLAYER_QUALIFICATION_SUSPENSION,2001,"
    })
    void createRejectsIllegalCombinations(String targetType, String penaltyCode, Long teamId, Long memberId) {
        DisciplineCreateReq req = baseReq(targetType, penaltyCode);
        req.setPlayerId(1001L);
        req.setTeamId(teamId);
        req.setMemberId(memberId);
        req.setEventTeamId(231L);
        req.setMatchSuspensionCount(2);
        req.setPointDeduction(3);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.create(req));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
        verify(disciplineRecordMapper, never()).insert(any());
    }

    @Test
    void createAllowsEventTeamDisqualificationDraft() {
        DisciplineCreateReq req = baseReq("EVENT_TEAM", "EVENT_DISQUALIFICATION");
        req.setEventTeamId(231L);
        req.setTeamId(2001L);
        stubSeason();
        when(disciplineRecordMapper.insert(any(BasketDisciplineRecord.class))).thenAnswer(invocation -> {
            BasketDisciplineRecord row = invocation.getArgument(0);
            row.setId(88L);
            return 1;
        });
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            assertEquals(88L, service.create(req));
        }
        verify(disciplineRecordMapper).insert(any(BasketDisciplineRecord.class));
    }

    @Test
    void createAllowsPlayerMatchSuspensionDraft() {
        DisciplineCreateReq req = baseReq("PLAYER", "MATCH_SUSPENSION");
        req.setPlayerId(1001L);
        req.setMatchSuspensionCount(2);
        stubSeason();
        when(disciplineRecordMapper.insert(any(BasketDisciplineRecord.class))).thenAnswer(invocation -> {
            BasketDisciplineRecord row = invocation.getArgument(0);
            row.setId(89L);
            return 1;
        });
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            assertEquals(89L, service.create(req));
        }
    }

    @Test
    void createAllowsPermanentTeamWarningDraft() {
        DisciplineCreateReq req = baseReq("PERMANENT_TEAM", "WARNING");
        req.setTeamId(2001L);
        stubSeason();
        when(disciplineRecordMapper.insert(any(BasketDisciplineRecord.class))).thenAnswer(invocation -> {
            BasketDisciplineRecord row = invocation.getArgument(0);
            row.setId(90L);
            return 1;
        });
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            assertEquals(90L, service.create(req));
        }
    }

    @Test
    void createAllowsPlayerCaptainBanDraft() {
        DisciplineCreateReq req = baseReq("PLAYER", "CAPTAIN_BAN");
        req.setPlayerId(1001L);
        stubSeason();
        when(disciplineRecordMapper.insert(any(BasketDisciplineRecord.class))).thenAnswer(invocation -> {
            BasketDisciplineRecord row = invocation.getArgument(0);
            row.setId(91L);
            return 1;
        });
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            assertEquals(91L, service.create(req));
        }
    }

    @Test
    void activateRejectsHistoricalPlayerDisqualificationDraft() {
        BasketDisciplineRecord record = new BasketDisciplineRecord();
        record.setId(10L);
        record.setTargetType("PLAYER");
        record.setPenaltyCode("EVENT_DISQUALIFICATION");
        record.setEventTeamId(231L);
        record.setPlayerId(1001L);
        record.setRecordStatus(DisciplineRecordStatusEnum.DRAFT.name());
        when(disciplineRecordMapper.selectById(10L)).thenReturn(record);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.activate(10L));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
        verify(disciplineRecordMapper, never()).updateRecordStatusIfMatch(
                any(), any(), any(), any(), any(), any());
    }

    private void stubSeason() {
        BasketSeason season = new BasketSeason();
        season.setId(130L);
        when(seasonMapper.selectById(130L)).thenReturn(season);
    }

    private static DisciplineCreateReq baseReq(String targetType, String penaltyCode) {
        DisciplineCreateReq req = new DisciplineCreateReq();
        req.setTargetType(targetType);
        req.setPenaltyCode(penaltyCode);
        req.setDisciplineType("DISCIPLINE");
        req.setTitle("兼容性测试");
        req.setReasonText("单元测试");
        req.setSeasonId(130L);
        return req;
    }
}
