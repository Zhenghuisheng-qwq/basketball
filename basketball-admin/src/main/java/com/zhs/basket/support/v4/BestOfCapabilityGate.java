package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.common.exception.ServiceException;

import java.util.List;

/**
 * BO3/BO5 能力门禁：配置层允许 LOCK，开放报名仍拦截；单败淘汰 BO3/BO5 赛程生成 R2-2 起放行。
 */
public final class BestOfCapabilityGate {

    private static final String OPEN_REGISTRATION_UNSUPPORTED_MSG =
            "当前赛制包含尚未支持的 BO3/BO5 阶段组合，暂不能开放报名。";
    private static final String OPEN_REGISTRATION_RR_BO_MSG =
            "阶段【%s】配置为循环积分赛 BO%d，当前版本尚未支持循环赛多局系列赛执行，暂不能开放报名。";
    private static final String OPEN_REGISTRATION_UNSUPPORTED_STAGE_MSG =
            "阶段【%s】配置为 BO%d 多局系列赛，当前版本尚未支持该赛制组合，暂不能开放报名。";
    private static final String GENERATE_SCHEDULE_MSG =
            "当前版本暂不支持 BO3/BO5 系列赛执行，请将 best_of 设为 1 后再生成赛程";
    private static final String ROUND_ROBIN_BO_MSG =
            "当前版本暂不支持循环积分 BO3/BO5 执行，请将 best_of 设为 1 后再生成赛程";

    private BestOfCapabilityGate() {
    }

    public static void assertCanOpenRegistration(List<BasketDivisionStage> stages) {
        if (stages == null) {
            return;
        }
        for (BasketDivisionStage stage : stages) {
            assertStageSupportedForOpenRegistration(stage);
        }
    }

    public static void assertCanOpenRegistrationBestOfValues(List<Integer> bestOfValues) {
        if (bestOfValues == null) {
            return;
        }
        for (Integer bestOf : bestOfValues) {
            if (bestOf != null && bestOf > 1) {
                throw new ServiceException(OPEN_REGISTRATION_UNSUPPORTED_MSG);
            }
        }
    }

    private static void assertStageSupportedForOpenRegistration(BasketDivisionStage stage) {
        if (stage == null || stage.getBestOf() == null || stage.getBestOf() <= 1) {
            return;
        }
        if (MatchSeriesExecutionCapability.supportsScheduleGeneration(stage)) {
            return;
        }
        String stageName = stage.getStageName() != null ? stage.getStageName() : stage.getStageCode();
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            throw new ServiceException(String.format(OPEN_REGISTRATION_RR_BO_MSG, stageName, stage.getBestOf()));
        }
        if (stageName != null && !stageName.isEmpty()) {
            throw new ServiceException(String.format(OPEN_REGISTRATION_UNSUPPORTED_STAGE_MSG, stageName, stage.getBestOf()));
        }
        throw new ServiceException(OPEN_REGISTRATION_UNSUPPORTED_MSG);
    }

    public static void assertCanGenerateSchedule(BasketDivisionStage stage) {
        if (stage == null || stage.getBestOf() == null || stage.getBestOf() <= 1) {
            return;
        }
        if (MatchSeriesExecutionCapability.supportsScheduleGeneration(stage)) {
            return;
        }
        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            throw new ServiceException(ROUND_ROBIN_BO_MSG);
        }
        throw new ServiceException(GENERATE_SCHEDULE_MSG);
    }

    public static boolean hasUnsupportedBestOf(BasketDivisionStage stage) {
        if (stage == null || stage.getBestOf() == null || stage.getBestOf() <= 1) {
            return false;
        }
        return !MatchSeriesExecutionCapability.supportsScheduleGeneration(stage);
    }
}
