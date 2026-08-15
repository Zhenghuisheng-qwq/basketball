package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * LEGACY Structure/Schedule 前置门禁。
 * VERSIONED Generate 走 {@link StructureGenerationPrerequisiteValidator}，不再用报名状态判断。
 */
@Component
public class StageSchedulePrerequisiteValidator {

    public static final String MSG_REGISTRATION_NOT_READY =
            "当前仍有报名未处理或正式参赛队伍尚未确认，暂不能生成赛程。";
    public static final String MSG_PUBLISHED_NO_MORE_TEAMS =
            "赛程已经发布，不能继续追加参赛队伍。如需调整，请先按赛事变更流程处理。";

    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Autowired
    private BasketDivisionRosterVersionMapper rosterVersionMapper;

    public void validateBeforeGenerateSchedule(BasketDivision division, BasketDivisionStage stage, boolean firstStage) {
        assertRegistrationReady(division);
        if (firstStage) {
            assertEventTeamsConfirmed(division, stage);
        }
    }

    public void validateBeforePublishSchedule(BasketDivision division, BasketDivisionStage stage, boolean firstStage) {
        assertRegistrationReady(division);
        int stageTeamCount = stageTeamMapper.countByStageId(stage.getId());
        if (stageTeamCount < 2) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        if (firstStage) {
            int eventTeamCount = currentFormalEventTeamCount(division);
            if (eventTeamCount != stageTeamCount) {
                throw businessError(MSG_REGISTRATION_NOT_READY);
            }
        }
    }

    public void assertRegistrationReady(BasketDivision division) {
        if (DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        if (registrationMapper.countPendingReviewByDivisionId(division.getId()) > 0) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        if (personalRegistrationMapper.countPendingReviewByDivisionId(division.getId()) > 0) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
    }

    private void assertEventTeamsConfirmed(BasketDivision division, BasketDivisionStage stage) {
        int eventTeamCount = currentFormalEventTeamCount(division);
        if (eventTeamCount < 2) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        BasketDivisionRosterVersion locked = rosterVersionMapper.selectCurrentLockedByDivisionId(division.getId());
        if (locked != null && locked.getFormalCount() != null) {
            if (eventTeamCount != locked.getFormalCount()) {
                throw businessError(MSG_REGISTRATION_NOT_READY);
            }
        } else if (RegistrationModeEnum.TEAM.getCode().equals(division.getRegistrationMode())) {
            int approvedCount = registrationMapper.countApprovedByDivisionId(division.getId());
            if (approvedCount != eventTeamCount) {
                throw businessError(MSG_REGISTRATION_NOT_READY);
            }
        }
        int stageTeamCount = stageTeamMapper.countByStageId(stage.getId());
        if (stageTeamCount > 0 && stageTeamCount != eventTeamCount) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
    }

    private int currentFormalEventTeamCount(BasketDivision division) {
        BasketDivisionRosterVersion locked = rosterVersionMapper.selectCurrentLockedByDivisionId(division.getId());
        if (locked != null && locked.getId() != null) {
            return eventTeamMapper.selectActiveByDivisionAndRosterVersion(division.getId(), locked.getId()).size();
        }
        return eventTeamMapper.countActiveByDivisionId(division.getId());
    }

    public ServiceException businessError(String message) {
        return new ServiceException(message, com.zhs.common.constant.HttpStatus.BAD_REQUEST);
    }
}
