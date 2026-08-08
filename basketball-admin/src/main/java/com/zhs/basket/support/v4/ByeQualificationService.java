package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.enums.v4.QualificationTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.support.v4.stage.EliminationByeSupport;
import com.zhs.basket.support.v4.stage.EliminationStageSupport;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 淘汰阶段轮空（BYE）晋级 PREVIEW 同步。
 */
@Component
public class ByeQualificationService {

    private static final int NOT_DELETED = 0;

    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;

    public BasketDivisionStageQualification buildByePreviewRow(BasketDivisionStage stage,
                                                               Long byeEventTeamId,
                                                               Long targetStageId,
                                                               Date now) {
        BasketDivisionStageQualification row = new BasketDivisionStageQualification();
        row.setDivisionId(stage.getDivisionId());
        row.setStageId(stage.getId());
        row.setEventTeamId(byeEventTeamId);
        row.setQualificationType(QualificationTypeEnum.BYE_ADVANCE.getCode());
        row.setSourceRank(0);
        row.setTargetStageId(targetStageId);
        row.setSourceSeriesId(null);
        row.setQualificationStatus(QualificationRecordStatusEnum.PREVIEW.getCode());
        row.setRemark("系统轮空晋级");
        row.setActiveUniqueKey(buildByeActiveKey(stage.getId(), byeEventTeamId,
                QualificationRecordStatusEnum.PREVIEW.getCode()));
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        return row;
    }

    public void upsertByePreview(BasketDivisionStage stage, List<BasketDivisionStageTeam> sortedTeams) {
        if (!EliminationByeSupport.allowsBye(stage, sortedTeams.size())) {
            return;
        }
        if (!EliminationStageSupport.isWinnerAdvanceNonFinalStage(stage)) {
            return;
        }
        Long byeTeamId = EliminationByeSupport.resolveByeEventTeamId(sortedTeams);
        BasketDivisionStageQualification existing = qualificationMapper.selectActiveByStageAndTeamAndType(
                stage.getId(), byeTeamId, QualificationTypeEnum.BYE_ADVANCE.getCode());
        if (existing != null && QualificationRecordStatusEnum.PREVIEW.getCode().equals(existing.getQualificationStatus())) {
            return;
        }
        if (existing != null) {
            throw new ServiceException("轮空晋级已确认，无法重复生成");
        }
        Long targetStageId = resolveTargetStageId(stage);
        Date now = new Date();
        BasketDivisionStageQualification row = buildByePreviewRow(stage, byeTeamId, targetStageId, now);
        if (qualificationMapper.insert(row) != 1) {
            throw new ServiceException("写入轮空晋级预览失败");
        }
    }

    public BasketDivisionStageQualification appendByePreviewIfNeeded(BasketDivisionStage stage,
                                                                      List<BasketDivisionStageTeam> stageTeams,
                                                                      List<BasketDivisionStageQualification> result,
                                                                      Date now) {
        List<BasketDivisionStageTeam> sorted = EliminationByeSupport.sortTeams(stageTeams);
        if (!EliminationByeSupport.allowsBye(stage, sorted.size())) {
            return null;
        }
        Long byeTeamId = EliminationByeSupport.resolveByeEventTeamId(sorted);
        boolean already = result.stream().anyMatch(r -> QualificationTypeEnum.BYE_ADVANCE.getCode().equals(r.getQualificationType())
                && byeTeamId.equals(r.getEventTeamId()));
        if (already) {
            return null;
        }
        Long targetStageId = resolveTargetStageId(stage);
        BasketDivisionStageQualification row = buildByePreviewRow(stage, byeTeamId, targetStageId, now);
        result.add(row);
        return row;
    }

    public static String buildByeActiveKey(Long stageId, Long eventTeamId, String status) {
        return stageId + ":BYE:" + status + ":" + eventTeamId;
    }

    private Long resolveTargetStageId(BasketDivisionStage stage) {
        if (!StringUtils.hasText(stage.getNextStageCode())) {
            return null;
        }
        return stageMapper.selectByDivisionId(stage.getDivisionId()).stream()
                .filter(s -> stage.getNextStageCode().equals(s.getStageCode()))
                .map(BasketDivisionStage::getId)
                .findFirst()
                .orElse(null);
    }
}
