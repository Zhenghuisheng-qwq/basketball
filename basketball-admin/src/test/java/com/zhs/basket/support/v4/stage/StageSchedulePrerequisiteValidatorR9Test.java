package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageSchedulePrerequisiteValidatorR9Test {

    @Mock private BasketRegistrationMapper registrationMapper;
    @Mock private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Mock private BasketEventTeamMapper eventTeamMapper;
    @Mock private BasketDivisionStageTeamMapper stageTeamMapper;
    @Mock private BasketDivisionRosterVersionMapper rosterVersionMapper;

    @InjectMocks
    private StageSchedulePrerequisiteValidator validator;

    private BasketDivision division;
    private BasketDivisionStage stage;

    @BeforeEach
    void setUp() {
        division = new BasketDivision();
        division.setId(3L);
        division.setDivisionStatus("REGISTRATION_CLOSED");
        division.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        stage = new BasketDivisionStage();
        stage.setId(11L);
        stage.setDivisionId(3L);
    }

    @Test
    void versionedTeamRoster_comparesFormalCountNotApprovedCount() {
        BasketDivisionRosterVersion locked = new BasketDivisionRosterVersion();
        locked.setId(21L);
        locked.setFormalCount(8);
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(3L)).thenReturn(locked);
        when(eventTeamMapper.selectActiveByDivisionAndRosterVersion(3L, 21L))
                .thenReturn(Collections.nCopies(8, new BasketEventTeam()));
        when(stageTeamMapper.countByStageId(11L)).thenReturn(0);

        assertDoesNotThrow(() -> validator.validateBeforeGenerateSchedule(division, stage, true));
    }

    @Test
    void personalLockedRoster_doesNotRequireTeamApprovedEqualsEventTeam() {
        division.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        BasketDivisionRosterVersion locked = new BasketDivisionRosterVersion();
        locked.setId(22L);
        locked.setFormalCount(2);
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(3L)).thenReturn(locked);
        when(eventTeamMapper.selectActiveByDivisionAndRosterVersion(3L, 22L))
                .thenReturn(Collections.nCopies(2, new BasketEventTeam()));
        when(stageTeamMapper.countByStageId(11L)).thenReturn(0);

        assertDoesNotThrow(() -> validator.validateBeforeGenerateSchedule(division, stage, true));
    }

    @Test
    void legacyTeam_stillRequiresApprovedEqualsEventTeam() {
        when(rosterVersionMapper.selectCurrentLockedByDivisionId(3L)).thenReturn(null);
        when(eventTeamMapper.countActiveByDivisionId(3L)).thenReturn(8);
        when(registrationMapper.countApprovedByDivisionId(3L)).thenReturn(10);

        assertThrows(com.zhs.common.exception.ServiceException.class,
                () -> validator.validateBeforeGenerateSchedule(division, stage, true));
    }
}