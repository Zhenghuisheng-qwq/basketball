package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionSafeRecoveryMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DivisionSafeRecoverySupportTest {

    @InjectMocks
    private DivisionSafeRecoverySupport support;

    @Mock
    private BasketDivisionSafeRecoveryMapper recoveryMapper;
    @Mock
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Mock
    private BasketDivisionStageMapper stageMapper;
    @Mock
    private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;

    @Test
    void case5_emptyDraftDeletesFormatConfig() {
        BasketDivision division = division(10L, DivisionStatusEnum.DRAFT.getCode());

        support.validateDivisionCanDelete(division);
        assertTrue(support.canDeleteDivision(division));
        support.softDeleteFormatConfig(10L);

        verify(tieBreakRuleMapper).softDeleteByDivisionId(10L);
        verify(stageMapper).softDeleteByDivisionId(10L);
        verify(snapshotMapper).softDeleteByDivisionId(10L);
    }

    @Test
    void case6_emptyRegistrationClosedCanDelete() {
        support.validateDivisionCanDelete(division(11L, DivisionStatusEnum.REGISTRATION_CLOSED.getCode()));
    }

    @Test
    void case7_emptyFinishedCanDelete() {
        support.validateDivisionCanDelete(division(12L, DivisionStatusEnum.FINISHED.getCode()));
    }

    @Test
    void case8_registrationBlocksDelete() {
        when(recoveryMapper.countTeamRegistration(13L)).thenReturn(1);
        assertBlocked(13L, DivisionStatusEnum.REGISTRATION_OPEN.getCode());
    }

    @Test
    void case9_eventTeamOrEventPlayerBlocksDelete() {
        when(recoveryMapper.countEventTeam(14L)).thenReturn(1);
        assertBlocked(14L, DivisionStatusEnum.FINISHED.getCode());

        when(recoveryMapper.countEventPlayer(15L)).thenReturn(1);
        assertBlocked(15L, DivisionStatusEnum.DRAFT.getCode());
    }

    @Test
    void case10_scheduleBlocksDelete() {
        when(recoveryMapper.countScheduleBatch(16L)).thenReturn(1);
        assertBlocked(16L, DivisionStatusEnum.REGISTRATION_CLOSED.getCode());
    }

    @Test
    void case11_matchSeriesOrMatchBlocksDelete() {
        when(recoveryMapper.countMatchSeries(17L)).thenReturn(1);
        assertBlocked(17L, DivisionStatusEnum.FINISHED.getCode());

        when(recoveryMapper.countMatch(18L)).thenReturn(1);
        assertBlocked(18L, DivisionStatusEnum.DRAFT.getCode());
    }

    @Test
    void case12_standingSubmissionOrStatsBlockDelete() {
        when(recoveryMapper.countStanding(19L)).thenReturn(1);
        assertBlocked(19L, DivisionStatusEnum.FINISHED.getCode());

        when(recoveryMapper.countSubmission(20L)).thenReturn(1);
        assertBlocked(20L, DivisionStatusEnum.FINISHED.getCode());

        when(recoveryMapper.countStats(21L)).thenReturn(1);
        assertBlocked(21L, DivisionStatusEnum.FINISHED.getCode());
    }

    @Test
    void playingScheduledAndArchivedCannotDeleteEvenWithoutBusinessRows() {
        assertStatusBlocked(22L, DivisionStatusEnum.PLAYING.getCode(), "比赛进行中的赛区不能删除，请使用正常赛事生命周期管理。");
        assertStatusBlocked(23L, DivisionStatusEnum.SCHEDULED.getCode(), "已排赛的赛区不能删除，请使用正常赛事生命周期管理。");
        assertStatusBlocked(24L, DivisionStatusEnum.ARCHIVED.getCode(), "已归档赛区不能删除");
    }

    @Test
    void case13_finishWithoutOfficialEvidenceIsRejected() {
        when(recoveryMapper.countCompletedOfficialMatches(30L)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> support.assertCanFinish(30L));

        assertEquals(DivisionSafeRecoverySupport.FINISH_BLOCKED_MESSAGE, ex.getMessage());
    }

    @Test
    void case14_finishWithOfficialEvidenceIsAllowed() {
        when(recoveryMapper.countCompletedOfficialMatches(31L)).thenReturn(1);

        support.assertCanFinish(31L);
    }

    @Test
    void finishCountsOnlyPublishedFinishedOrForfeitedMatches() throws Exception {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/basket/v4/BasketDivisionSafeRecoveryMapper.xml"));
        int start = xml.indexOf("id=\"countCompletedOfficialMatches\"");
        int end = xml.indexOf("</select>", start);
        String sql = xml.substring(start, end);
        assertTrue(sql.contains("basket_match"));
        assertTrue(sql.contains("'FINISHED'"));
        assertTrue(sql.contains("'FORFEITED'"));
        assertTrue(sql.contains("publish_status = 'PUBLISHED'"));
        assertTrue(!sql.contains("basket_schedule_batch"));
        assertTrue(!sql.contains("basket_match_series"));
        assertTrue(!sql.contains("'CANCELLED'"));
        assertTrue(!sql.contains("'PLAYING'"));
        assertTrue(!sql.contains("'SCHEDULED'"));
    }

    @Test
    void caseA_publishedScheduleWithoutMatchCannotFinish() {
        assertFinishRejected(41L);
    }

    @Test
    void caseB_unplayedMatchSeriesCannotFinish() {
        assertFinishRejected(42L);
    }

    @Test
    void caseC_unfinishedMatchesCannotFinish() {
        assertFinishRejected(43L);
    }

    @Test
    void caseD_completedOfficialMatchCanFinish() {
        when(recoveryMapper.countCompletedOfficialMatches(44L)).thenReturn(1);

        support.assertCanFinish(44L);
    }

    private void assertFinishRejected(Long divisionId) {
        when(recoveryMapper.countCompletedOfficialMatches(divisionId)).thenReturn(0);
        ServiceException ex = assertThrows(ServiceException.class, () -> support.assertCanFinish(divisionId));
        assertEquals("当前赛区仍没有已完成的正式比赛，不能结束赛事。", ex.getMessage());
    }

    private void assertBlocked(Long id, String status) {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> support.validateDivisionCanDelete(division(id, status)));
        assertEquals(DivisionSafeRecoverySupport.DELETE_BLOCKED_MESSAGE, ex.getMessage());
        assertFalse(support.canDeleteDivision(division(id, status)));
    }

    private void assertStatusBlocked(Long id, String status, String message) {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> support.validateDivisionCanDelete(division(id, status)));
        assertEquals(message, ex.getMessage());
    }

    private BasketDivision division(Long id, String status) {
        BasketDivision division = new BasketDivision();
        division.setId(id);
        division.setDivisionStatus(status);
        return division;
    }
}
