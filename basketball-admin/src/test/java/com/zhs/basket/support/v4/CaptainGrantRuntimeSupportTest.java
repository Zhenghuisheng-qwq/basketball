package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.enums.TeamStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptainGrantRuntimeSupportTest {

    @Mock
    private BasketCaptainGrantMapper captainGrantMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketSeasonMapper seasonMapper;
    @Mock
    private BasketTeamMapper teamMapper;
    @Mock
    private CaptainGrantLogSupport grantLogSupport;

    @InjectMocks
    private CaptainGrantRuntimeSupport runtimeSupport;

    @Test
    void bindTeamIfAbsent_allowsFirstBind() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        BasketTeam team = activeTeam(99L, 118L);
        when(teamMapper.selectById(99L)).thenReturn(team);
        when(captainGrantMapper.bindTeamById(1L, 99L)).thenReturn(1);

        assertDoesNotThrow(() -> runtimeSupport.bindTeamIfAbsent(grant, 99L, "MEMBER"));
    }

    @Test
    void bindTeamIfAbsent_rejectsLegacyTeam() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        BasketTeam team = activeTeam(99L, null);
        when(teamMapper.selectById(99L)).thenReturn(team);

        assertThrows(ServiceException.class, () -> runtimeSupport.bindTeamIfAbsent(grant, 99L, "MEMBER"));
    }

    @Test
    void bindTeamIfAbsent_rejectsArchivedTeam() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        BasketTeam team = activeTeam(99L, 118L);
        team.setTeamStatus(TeamStatusEnum.ARCHIVED.getCode());
        when(teamMapper.selectById(99L)).thenReturn(team);

        assertThrows(ServiceException.class, () -> runtimeSupport.bindTeamIfAbsent(grant, 99L, "MEMBER"));
    }

    @Test
    void bindTeamIfAbsent_rejectsDivisionMismatch() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        BasketTeam team = activeTeam(99L, 120L);
        when(teamMapper.selectById(99L)).thenReturn(team);

        assertThrows(ServiceException.class, () -> runtimeSupport.bindTeamIfAbsent(grant, 99L, "MEMBER"));
    }

    @Test
    void bindTeamIfAbsent_rejectsOtherTeam() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        grant.setTeamId(88L);
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        BasketTeam team = activeTeam(99L, 118L);
        when(teamMapper.selectById(99L)).thenReturn(team);

        assertThrows(ServiceException.class, () -> runtimeSupport.bindTeamIfAbsent(grant, 99L, "MEMBER"));
    }

    @Test
    void assertTeamBindingForRegistration_rejectsConflict() {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(118L);
        grant.setTeamId(88L);
        BasketTeam team = activeTeam(99L, 118L);

        assertThrows(ServiceException.class, () -> runtimeSupport.assertTeamBindingForRegistration(grant, team));
    }

    private static BasketTeam activeTeam(Long teamId, Long divisionId) {
        BasketTeam team = new BasketTeam();
        team.setId(teamId);
        team.setDivisionId(divisionId);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        return team;
    }
}
