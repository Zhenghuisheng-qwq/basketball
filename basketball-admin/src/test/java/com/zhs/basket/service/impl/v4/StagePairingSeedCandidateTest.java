package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.dto.v4.StagePairingSeedReq;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingExecutionMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagePairingSeedCandidateTest {

    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketDivisionStageMapper stageMapper;
    @Mock
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Mock
    private BasketStagePairingExecutionMapper executionMapper;
    @Mock
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private TeamRosterSupport teamRosterSupport;

    @InjectMocks
    private StagePairingExecutionApplicationServiceImpl service;

    @Test
    void emptyWorkspace_zeroFormal_participantCountZero() {
        BasketDivisionStage stage = stage(637L, 174L);
        when(stageMapper.selectById(637L)).thenReturn(stage);
        when(executionMapper.selectCurrentByStageId(637L)).thenReturn(null);
        when(stageTeamMapper.selectByStageId(637L)).thenReturn(List.of());
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(List.of());

        StagePairingExecutionVo vo = service.getCurrent(637L);
        assertEquals(0, vo.getParticipantCount());
        assertNull(vo.getBracketCapacity());
    }

    @Test
    void emptyWorkspace_eightFormal_participantCountEight() {
        BasketDivisionStage stage = stage(637L, 174L);
        when(stageMapper.selectById(637L)).thenReturn(stage);
        when(executionMapper.selectCurrentByStageId(637L)).thenReturn(null);
        when(stageTeamMapper.selectByStageId(637L)).thenReturn(List.of());
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(eventTeams(8));

        StagePairingExecutionVo vo = service.getCurrent(637L);
        assertEquals(8, vo.getParticipantCount());
        assertEquals(8, vo.getBracketCapacity());
        assertEquals(0, vo.getByeCount());
    }

    @Test
    void adminSeed_moreThanFour_rejected() {
        BasketDivisionStage stage = stage(637L, 174L);
        when(stageMapper.selectById(637L)).thenReturn(stage);
        when(divisionMapper.selectByIdForUpdate(174L)).thenReturn(new BasketDivision());
        when(scheduleBatchMapper.selectPublishedByStageId(637L)).thenReturn(null);
        when(matchMapper.countFinishedByStageId(637L)).thenReturn(0);
        when(executionMapper.selectCurrentByStageId(637L)).thenReturn(null);
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(eventTeams(8));
        when(stageTeamMapper.selectByStageId(637L)).thenReturn(stageTeams(637L, 8));

        StagePairingSeedReq req = new StagePairingSeedReq();
        req.setSeedEventTeamIds(List.of(1L, 2L, 3L, 4L, 5L));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveAdminSeeds(637L, req));
        assertTrue(ex.getMessage().contains("4"));
    }

    private static BasketDivisionStage stage(Long id, Long divisionId) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(id);
        stage.setDivisionId(divisionId);
        stage.setSnapshotId(268L);
        stage.setStageOrder(1);
        stage.setSeedRule("ADMIN_SEED");
        stage.setDrawRule("RANDOM_CONFIRM");
        stage.setByeRule("SEED_PRIORITY");
        return stage;
    }

    private static List<BasketEventTeam> eventTeams(int count) {
        List<BasketEventTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketEventTeam team = new BasketEventTeam();
            team.setId((long) i);
            team.setTeamNameSnapshot("FORMAL-" + i);
            teams.add(team);
        }
        return teams;
    }

    private static List<BasketDivisionStageTeam> stageTeams(Long stageId, int count) {
        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketDivisionStageTeam team = new BasketDivisionStageTeam();
            team.setStageId(stageId);
            team.setEventTeamId((long) i);
            team.setSeedNo(i);
            teams.add(team);
        }
        return teams;
    }
}
