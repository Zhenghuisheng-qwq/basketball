package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.GroupingStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DivisionGroupingLockSpecification {

    @Autowired
    private BasketDivisionGroupMapper groupMapper;
    @Autowired
    private BasketDivisionGroupTeamMapper groupTeamMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;

    public void validate(BasketDivisionGroupingBatch batch, BasketDivisionStage stage) {
        if (batch == null) {
            throw new ServiceException("分组批次不存在");
        }
        if (!GroupingStatusEnum.DRAFT.getCode().equals(batch.getGroupingStatus())) {
            throw new ServiceException("仅草稿批次可锁定");
        }
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        if (!batch.getDivisionId().equals(stage.getDivisionId())) {
            throw new ServiceException("阶段不属于该 Division");
        }
        if (!batch.getStageId().equals(stage.getId())) {
            throw new ServiceException("批次与阶段不匹配");
        }

        GroupingModeEnum groupingMode = GroupingModeEnum.fromCode(stage.getGroupingMode());
        if (groupingMode == GroupingModeEnum.NONE) {
            throw new ServiceException("该阶段无需分组");
        }
        if (stage.getGroupCount() == null || stage.getGroupCount() <= 0) {
            throw new ServiceException("阶段未配置有效小组数量");
        }

        int actualGroupCount = groupMapper.countByBatchId(batch.getId());
        if (actualGroupCount != stage.getGroupCount()) {
            throw new ServiceException("小组数量与阶段配置不一致，期望 " + stage.getGroupCount() + " 实际 " + actualGroupCount);
        }

        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(batch.getDivisionId());
        if (snapshot == null) {
            throw new ServiceException("Division 缺少规则快照");
        }
        SnapshotStatusEnum snapshotStatus = SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus());
        if (snapshotStatus == null || !snapshotStatus.isLockedOrBeyond()) {
            throw new ServiceException("规则快照尚未锁定，无法锁定分组");
        }

        if (matchMapper.countByDivisionIdAndStageCode(batch.getDivisionId(), stage.getStageCode()) > 0) {
            throw new ServiceException("该阶段已生成赛程，无法锁定分组");
        }

        List<BasketEventTeam> requiredTeams = eventTeamMapper.selectActiveByDivisionId(batch.getDivisionId());
        List<BasketDivisionGroupTeam> assignments = groupTeamMapper.selectByBatchId(batch.getId());
        Set<Long> assignedEventTeamIds = new HashSet<>();

        for (BasketDivisionGroup group : groupMapper.selectByBatchId(batch.getId())) {
            int teamCountInGroup = groupTeamMapper.countByGroupId(group.getId());
            if (teamCountInGroup == 0) {
                throw new ServiceException("小组「" + group.getGroupName() + "」不能为空");
            }
            if (AdvancementTypeEnum.GROUP_TOP_N.getCode().equals(stage.getAdvancementType())) {
                Integer qualifyCount = group.getQualifyCount();
                if (qualifyCount == null || qualifyCount <= 0) {
                    throw new ServiceException("小组「" + group.getGroupName() + "」需配置晋级名额");
                }
            }
        }

        for (BasketDivisionGroupTeam assignment : assignments) {
            if (!assignedEventTeamIds.add(assignment.getEventTeamId())) {
                throw new ServiceException("参赛球队不能重复分配");
            }
            if (!batch.getDivisionId().equals(assignment.getDivisionId())) {
                throw new ServiceException("存在跨 Division 的参赛球队分配");
            }
            BasketEventTeam eventTeam = eventTeamMapper.selectById(assignment.getEventTeamId());
            if (eventTeam == null || !batch.getDivisionId().equals(eventTeam.getDivisionId())) {
                throw new ServiceException("参赛球队不存在或不属于该 Division");
            }
        }

        if (assignedEventTeamIds.size() != requiredTeams.size()) {
            throw new ServiceException("尚有 " + (requiredTeams.size() - assignedEventTeamIds.size()) + " 支参赛球队未分配");
        }
        for (BasketEventTeam team : requiredTeams) {
            if (!assignedEventTeamIds.contains(team.getId())) {
                throw new ServiceException("参赛球队「" + team.getTeamNameSnapshot() + "」未分配");
            }
        }
    }
}
