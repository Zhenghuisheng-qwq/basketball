package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionSafeRecoveryMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 误创建 / 误发布 / 误结束的安全收回。
 * 结束赛事至少要有一场已发布且状态为 FINISHED 或 FORFEITED 的比赛。
 * 删除只看是否已产生正式业务数据。
 * 创建赛区时自动生成的规则快照、阶段、同分规则不算正式业务数据。
 */
@Component
public class DivisionSafeRecoverySupport {

    public static final String FINISH_BLOCKED_MESSAGE =
            "当前赛区仍没有已完成的正式比赛，不能结束赛事。";
    public static final String DELETE_BLOCKED_MESSAGE =
            "赛区已经产生正式业务数据，不能删除，请使用正常赛事生命周期管理。";

    private static final Set<String> DELETABLE_STATUSES = Set.of(
            DivisionStatusEnum.DRAFT.getCode(),
            DivisionStatusEnum.REGISTRATION_OPEN.getCode(),
            DivisionStatusEnum.REGISTRATION_CLOSED.getCode(),
            DivisionStatusEnum.FINISHED.getCode()
    );

    @Autowired
    private BasketDivisionSafeRecoveryMapper recoveryMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionTieBreakRuleMapper tieBreakRuleMapper;

    public boolean canDeleteDivision(BasketDivision division) {
        try {
            validateDivisionCanDelete(division);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    public void validateDivisionCanDelete(BasketDivision division) {
        if (division == null || division.getId() == null) {
            throw new ServiceException("Division 不存在");
        }
        String status = division.getDivisionStatus();
        if (DivisionStatusEnum.ARCHIVED.getCode().equals(status)) {
            throw new ServiceException("已归档赛区不能删除");
        }
        if (DivisionStatusEnum.PLAYING.getCode().equals(status)) {
            throw new ServiceException("比赛进行中的赛区不能删除，请使用正常赛事生命周期管理。");
        }
        if (DivisionStatusEnum.SCHEDULED.getCode().equals(status)) {
            throw new ServiceException("已排赛的赛区不能删除，请使用正常赛事生命周期管理。");
        }
        if (!DELETABLE_STATUSES.contains(status)) {
            throw new ServiceException("当前赛区状态不允许删除");
        }
        if (hasFormalBusinessData(division.getId())) {
            throw new ServiceException(DELETE_BLOCKED_MESSAGE);
        }
    }

    public void assertCanFinish(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        if (recoveryMapper.countCompletedOfficialMatches(divisionId) <= 0) {
            throw new ServiceException(FINISH_BLOCKED_MESSAGE);
        }
    }

    /**
     * 软删创建赛区时自动生成的赛制配置，避免留下孤儿快照。
     */
    public void softDeleteFormatConfig(Long divisionId) {
        tieBreakRuleMapper.softDeleteByDivisionId(divisionId);
        stageMapper.softDeleteByDivisionId(divisionId);
        snapshotMapper.softDeleteByDivisionId(divisionId);
    }

    private boolean hasFormalBusinessData(Long divisionId) {
        return recoveryMapper.countTeamRegistration(divisionId) > 0
                || recoveryMapper.countPersonalRegistration(divisionId) > 0
                || recoveryMapper.countRoster(divisionId) > 0
                || recoveryMapper.countEventTeam(divisionId) > 0
                || recoveryMapper.countEventPlayer(divisionId) > 0
                || recoveryMapper.countCaptainGrant(divisionId) > 0
                || recoveryMapper.countScheduleBatch(divisionId) > 0
                || recoveryMapper.countMatchSeries(divisionId) > 0
                || recoveryMapper.countMatch(divisionId) > 0
                || recoveryMapper.countStanding(divisionId) > 0
                || recoveryMapper.countStats(divisionId) > 0
                || recoveryMapper.countSubmission(divisionId) > 0
                || recoveryMapper.countQualification(divisionId) > 0;
    }
}
