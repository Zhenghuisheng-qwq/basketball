package com.zhs.support;

import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketDivision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamWritableSupportTest {

    @Mock
    private BasketDivisionDao divisionDao;

    @InjectMocks
    private TeamWritableSupport support;

    private BasketDivision openDivision;

    @BeforeEach
    void setUp() {
        openDivision = new BasketDivision();
        openDivision.setId(118L);
        openDivision.setIsDelete(0);
        openDivision.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
    }

    @Test
    void assertTeamWritable_rejectsLegacy() {
        BasketTeam team = activeTeam(null);
        assertThrows(ServiceException.class, () -> support.assertTeamWritable(team));
    }

    @Test
    void assertTeamWritable_rejectsArchived() {
        BasketTeam team = activeTeam(118L);
        team.setTeamStatus(TeamStatusEnum.ARCHIVED.getCode());
        assertThrows(ServiceException.class, () -> support.assertTeamWritable(team));
    }

    @Test
    void assertTeamWritable_allowsActiveDivisionTeam() {
        BasketTeam team = activeTeam(118L);
        when(divisionDao.selectById(118L)).thenReturn(openDivision);
        assertDoesNotThrow(() -> support.assertTeamWritable(team));
    }

    @Test
    void requireDivisionForTeamCreate_rejectsFinished() {
        BasketDivision finished = new BasketDivision();
        finished.setId(118L);
        finished.setIsDelete(0);
        finished.setDivisionStatus(DivisionStatusEnum.FINISHED.getCode());
        when(divisionDao.selectById(118L)).thenReturn(finished);
        assertThrows(ServiceException.class, () -> support.requireDivisionForTeamCreate(118L));
    }

    private BasketTeam activeTeam(Long divisionId) {
        BasketTeam team = new BasketTeam();
        team.setId(1L);
        team.setIsDelete(0);
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        team.setDivisionId(divisionId);
        return team;
    }
}
