package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.vo.v4.BasketStageTeamItemVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DivisionStageExecutionListStageTeamsTest {

    @Mock
    private BasketDivisionStageMapper stageMapper;
    @Mock
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Mock
    private TeamRosterSupport teamRosterSupport;

    @InjectMocks
    private DivisionStageExecutionApplicationServiceImpl service;

    @Test
    void firstStage_lockedRosterZeroFormal_seedCandidatesEmpty() {
        BasketDivisionStage stage = stage(637L, 174L);
        when(stageMapper.selectById(637L)).thenReturn(stage);
        when(stageTeamMapper.selectByStageId(637L)).thenReturn(List.of());
        when(stageMapper.selectFirstByDivisionId(174L)).thenReturn(stage);
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(List.of());

        assertTrue(service.listStageTeams(637L).isEmpty());
    }

    @Test
    void firstStage_eightFormal_excludesCandidates() {
        BasketDivisionStage stage = stage(637L, 174L);
        when(stageMapper.selectById(637L)).thenReturn(stage);
        when(stageTeamMapper.selectByStageId(637L)).thenReturn(List.of());
        when(stageMapper.selectFirstByDivisionId(174L)).thenReturn(stage);
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(8));

        List<BasketStageTeamItemVo> items = service.listStageTeams(637L);
        assertEquals(8, items.size());
        Set<Long> ids = items.stream().map(BasketStageTeamItemVo::getEventTeamId).collect(Collectors.toSet());
        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), ids);
        assertTrue(items.stream().noneMatch(item -> item.getTeamName() != null && item.getTeamName().startsWith("CANDIDATE")));
    }

    private static BasketDivisionStage stage(Long id, Long divisionId) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(id);
        stage.setDivisionId(divisionId);
        stage.setSnapshotId(268L);
        stage.setStageOrder(1);
        return stage;
    }

    private static List<BasketEventTeam> formalTeams(int count) {
        List<BasketEventTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketEventTeam team = new BasketEventTeam();
            team.setId((long) i);
            team.setTeamNameSnapshot("FORMAL-" + i);
            teams.add(team);
        }
        return teams;
    }
}
