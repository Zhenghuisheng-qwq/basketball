package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 赛程生成/发布前的前置门禁：报名关闭、无待审、参赛队伍已确认。
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
            int eventTeamCount = eventTeamMapper.countActiveByDivisionId(division.getId());
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
        int eventTeamCount = eventTeamMapper.countActiveByDivisionId(division.getId());
        if (eventTeamCount < 2) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        int approvedCount = registrationMapper.countApprovedByDivisionId(division.getId());
        if (approvedCount != eventTeamCount) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
        int stageTeamCount = stageTeamMapper.countByStageId(stage.getId());
        if (stageTeamCount > 0 && stageTeamCount != eventTeamCount) {
            throw businessError(MSG_REGISTRATION_NOT_READY);
        }
    }

    public ServiceException businessError(String message) {
        return new ServiceException(message, com.zhs.common.constant.HttpStatus.BAD_REQUEST);
    }
}
