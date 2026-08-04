package com.zhs.basket.service.impl.v4;



import com.zhs.basket.domain.v4.*;

import com.zhs.basket.dto.v4.BasketDivisionGroupTeamAssignReq;

import com.zhs.basket.dto.v4.BasketDivisionGroupUpdateReq;

import com.zhs.basket.dto.v4.BasketDivisionGroupingBatchCreateReq;

import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;

import com.zhs.basket.enums.v4.GroupingResultSourceEnum;

import com.zhs.basket.enums.v4.GroupingStatusEnum;

import com.zhs.basket.mapper.v4.*;

import com.zhs.basket.query.v4.BasketDivisionGroupingBatchQuery;

import com.zhs.basket.service.v4.IDivisionGroupingApplicationService;

import com.zhs.basket.support.v4.DivisionGroupingLockSpecification;

import com.zhs.basket.vo.v4.BasketDivisionGroupVo;

import com.zhs.basket.vo.v4.BasketDivisionGroupingBatchDetailVo;

import com.zhs.basket.vo.v4.BasketDivisionGroupingBatchListVo;

import com.zhs.common.exception.ServiceException;

import com.zhs.common.utils.SecurityUtils;

import com.zhs.common.utils.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.util.Date;

import java.util.List;



@Service

public class DivisionGroupingApplicationServiceImpl implements IDivisionGroupingApplicationService {



    private static final Integer NOT_DELETED = 0;



    @Autowired

    private BasketDivisionGroupingBatchMapper groupingBatchMapper;

    @Autowired

    private BasketDivisionGroupMapper groupMapper;

    @Autowired

    private BasketDivisionGroupTeamMapper groupTeamMapper;

    @Autowired

    private BasketDivisionMapper divisionMapper;

    @Autowired

    private BasketDivisionStageMapper stageMapper;

    @Autowired

    private BasketEventTeamMapper eventTeamMapper;

    @Autowired

    private BasketV4MatchMapper matchMapper;

    @Autowired

    private DivisionGroupingLockSpecification lockSpecification;



    @Override

    public List<BasketDivisionGroupingBatchListVo> selectBatchList(BasketDivisionGroupingBatchQuery query) {

        return groupingBatchMapper.selectVoList(query);

    }



    @Override

    public BasketDivisionGroupingBatchDetailVo selectBatchDetail(Long groupingBatchId) {

        BasketDivisionGroupingBatch batch = requireBatch(groupingBatchId);

        BasketDivisionStage stage = requireStage(batch.getStageId());



        BasketDivisionGroupingBatchDetailVo vo = new BasketDivisionGroupingBatchDetailVo();

        vo.setGroupingBatchId(batch.getId());

        vo.setDivisionId(batch.getDivisionId());

        vo.setStageId(batch.getStageId());

        vo.setStageCode(stage.getStageCode());

        vo.setStageName(stage.getStageName());

        vo.setBatchVersion(batch.getBatchVersion());

        vo.setGroupingMode(batch.getGroupingMode());

        vo.setGroupingStatus(batch.getGroupingStatus());

        vo.setGroupCount(batch.getGroupCount());

        vo.setResultSource(batch.getResultSource());

        vo.setConfirmedTime(batch.getConfirmedTime());



        BasketDivision division = divisionMapper.selectById(batch.getDivisionId());

        if (division != null) {

            vo.setDivisionName(division.getDivisionName());

        }



        List<BasketDivisionGroupVo> groups = groupMapper.selectVoByBatchId(groupingBatchId);

        for (BasketDivisionGroupVo group : groups) {

            group.setTeams(groupTeamMapper.selectVoByGroupId(group.getGroupId()));

        }

        vo.setGroups(groups);

        return vo;

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public Long createDraftBatch(BasketDivisionGroupingBatchCreateReq req) {

        if (req == null || req.getDivisionId() == null || req.getStageId() == null) {

            throw new ServiceException("divisionId 与 stageId 不能为空");

        }

        BasketDivision division = requireDivision(req.getDivisionId());

        BasketDivisionStage stage = requireStage(req.getStageId());

        if (!division.getId().equals(stage.getDivisionId())) {

            throw new ServiceException("阶段不属于该 Division");

        }



        GroupingModeEnum groupingMode = GroupingModeEnum.fromCode(stage.getGroupingMode());

        if (groupingMode == GroupingModeEnum.NONE) {

            throw new ServiceException("该阶段无需分组");

        }

        if (stage.getGroupCount() == null || stage.getGroupCount() <= 0) {

            throw new ServiceException("阶段未配置有效小组数量");

        }



        BasketDivisionGroupingBatch active = groupingBatchMapper.selectActiveByDivisionAndStage(

                division.getId(), stage.getId());

        if (active != null && !GroupingStatusEnum.CANCELLED.getCode().equals(active.getGroupingStatus())) {

            throw new ServiceException("该阶段已有进行中的分组批次");

        }



        int version = groupingBatchMapper.selectNextBatchVersion(division.getId(), stage.getId());

        Date now = new Date();

        Long operatorId = SecurityUtils.getUserId();



        BasketDivisionGroupingBatch batch = new BasketDivisionGroupingBatch();

        batch.setDivisionId(division.getId());

        batch.setStageId(stage.getId());

        batch.setBatchVersion(version);

        batch.setGroupingMode(stage.getGroupingMode());

        batch.setGroupingStatus(GroupingStatusEnum.DRAFT.getCode());

        batch.setActiveUniqueKey("DIVISION:" + division.getId() + ":STAGE:" + stage.getId());

        batch.setGroupCount(stage.getGroupCount());

        batch.setResultSource(StringUtils.isNotEmpty(req.getResultSource())

                ? req.getResultSource() : GroupingResultSourceEnum.MANUAL.getCode());

        batch.setCreateBy(operatorId);

        batch.setUpdateBy(operatorId);

        batch.setCreateTime(now);

        batch.setUpdateTime(now);

        batch.setIsDelete(NOT_DELETED);

        if (groupingBatchMapper.insert(batch) != 1) {

            throw new ServiceException("创建分组批次失败");

        }



        createDefaultGroups(batch, stage, now);

        return batch.getId();

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void updateGroup(Long groupingBatchId, Long groupId, BasketDivisionGroupUpdateReq req) {

        BasketDivisionGroupingBatch batch = requireDraftBatch(groupingBatchId);

        BasketDivisionGroup group = requireGroup(groupId, batch.getId());

        if (req == null) {

            throw new ServiceException("请求体不能为空");

        }

        if (StringUtils.isNotEmpty(req.getGroupName())) {

            group.setGroupName(req.getGroupName().trim());

        }

        if (req.getQualifyCount() != null) {

            group.setQualifyCount(req.getQualifyCount());

        }

        group.setUpdateTime(new Date());

        if (groupMapper.updateById(group) != 1) {

            throw new ServiceException("更新小组失败");

        }

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public Long assignEventTeam(Long groupingBatchId, Long groupId, BasketDivisionGroupTeamAssignReq req) {

        BasketDivisionGroupingBatch batch = requireDraftBatch(groupingBatchId);

        BasketDivisionGroup group = requireGroup(groupId, batch.getId());

        if (req == null || req.getEventTeamId() == null) {

            throw new ServiceException("eventTeamId 不能为空");

        }



        BasketEventTeam eventTeam = eventTeamMapper.selectById(req.getEventTeamId());

        if (eventTeam == null || !batch.getDivisionId().equals(eventTeam.getDivisionId())) {

            throw new ServiceException("参赛球队不存在或不属于该 Division");

        }



        List<BasketDivisionGroupTeam> existing = groupTeamMapper.selectByBatchId(batch.getId());

        for (BasketDivisionGroupTeam assignment : existing) {

            if (assignment.getEventTeamId().equals(req.getEventTeamId())) {

                if (assignment.getGroupId().equals(groupId)) {

                    return assignment.getId();

                }

                assignment.setGroupId(groupId);

                if (req.getSeedNo() != null) {

                    assignment.setSeedNo(req.getSeedNo());

                }

                if (req.getDrawOrder() != null) {

                    assignment.setDrawOrder(req.getDrawOrder());

                }

                if (StringUtils.isNotEmpty(req.getAssignmentSource())) {

                    assignment.setAssignmentSource(req.getAssignmentSource());

                }

                assignment.setUpdateTime(new Date());

                if (groupTeamMapper.updateById(assignment) != 1) {

                    throw new ServiceException("移动参赛球队失败");

                }

                return assignment.getId();

            }

        }



        Date now = new Date();

        BasketDivisionGroupTeam assignment = new BasketDivisionGroupTeam();

        assignment.setGroupingBatchId(batch.getId());

        assignment.setDivisionId(batch.getDivisionId());

        assignment.setStageId(batch.getStageId());

        assignment.setGroupId(group.getId());

        assignment.setEventTeamId(req.getEventTeamId());

        assignment.setSeedNo(req.getSeedNo());

        assignment.setDrawOrder(req.getDrawOrder());

        assignment.setAssignmentSource(StringUtils.isNotEmpty(req.getAssignmentSource())

                ? req.getAssignmentSource() : GroupingResultSourceEnum.MANUAL.getCode());

        assignment.setCreateTime(now);

        assignment.setUpdateTime(now);

        assignment.setIsDelete(NOT_DELETED);

        if (groupTeamMapper.insert(assignment) != 1) {

            throw new ServiceException("分配参赛球队失败");

        }

        return assignment.getId();

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void removeAssignment(Long groupingBatchId, Long assignmentId) {

        requireDraftBatch(groupingBatchId);

        BasketDivisionGroupTeam assignment = requireAssignment(assignmentId, groupingBatchId);

        groupTeamMapper.softDelete(assignment.getId());

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void lockBatch(Long groupingBatchId) {

        BasketDivisionGroupingBatch batch = requireBatch(groupingBatchId);

        BasketDivisionStage stage = requireStage(batch.getStageId());

        lockSpecification.validate(batch, stage);



        Date now = new Date();

        Long operatorId = SecurityUtils.getUserId();

        batch.setGroupingStatus(GroupingStatusEnum.LOCKED.getCode());

        batch.setConfirmedBy(operatorId);

        batch.setConfirmedTime(now);

        batch.setUpdateBy(operatorId);

        batch.setUpdateTime(now);

        if (groupingBatchMapper.updateById(batch) != 1) {

            throw new ServiceException("锁定分组批次失败");

        }

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void cancelBatch(Long groupingBatchId) {

        BasketDivisionGroupingBatch batch = requireBatch(groupingBatchId);

        if (!GroupingStatusEnum.DRAFT.getCode().equals(batch.getGroupingStatus())) {

            throw new ServiceException("仅草稿批次可取消");

        }

        BasketDivisionStage stage = requireStage(batch.getStageId());

        if (matchMapper.countByDivisionIdAndStageCode(batch.getDivisionId(), stage.getStageCode()) > 0) {

            throw new ServiceException("该阶段已生成赛程，无法取消分组批次");

        }

        batch.setGroupingStatus(GroupingStatusEnum.CANCELLED.getCode());

        batch.setActiveUniqueKey(null);

        batch.setUpdateBy(SecurityUtils.getUserId());

        batch.setUpdateTime(new Date());

        if (groupingBatchMapper.updateById(batch) != 1) {

            throw new ServiceException("取消分组批次失败");

        }

    }



    private void createDefaultGroups(BasketDivisionGroupingBatch batch, BasketDivisionStage stage, Date now) {

        int groupCount = stage.getGroupCount();

        for (int i = 0; i < groupCount; i++) {

            String groupCode = toGroupCode(i);

            BasketDivisionGroup group = new BasketDivisionGroup();

            group.setGroupingBatchId(batch.getId());

            group.setDivisionId(batch.getDivisionId());

            group.setStageId(batch.getStageId());

            group.setGroupCode(groupCode);

            group.setGroupName(groupCode + "组");

            group.setGroupOrder(i + 1);

            if (AdvancementTypeEnum.GROUP_TOP_N.getCode().equals(stage.getAdvancementType())) {

                group.setQualifyCount(stage.getAdvancementCount());

            }

            group.setCreateTime(now);

            group.setUpdateTime(now);

            group.setIsDelete(NOT_DELETED);

            if (groupMapper.insert(group) != 1) {

                throw new ServiceException("创建默认小组失败");

            }

        }

    }



    private static String toGroupCode(int index) {

        if (index < 26) {

            return String.valueOf((char) ('A' + index));

        }

        return toGroupCode(index / 26 - 1) + (char) ('A' + index % 26);

    }



    private BasketDivisionGroupingBatch requireBatch(Long batchId) {

        if (batchId == null) {

            throw new ServiceException("groupingBatchId 不能为空");

        }

        BasketDivisionGroupingBatch batch = groupingBatchMapper.selectById(batchId);

        if (batch == null) {

            throw new ServiceException("分组批次不存在");

        }

        return batch;

    }



    private BasketDivisionGroupingBatch requireDraftBatch(Long batchId) {

        BasketDivisionGroupingBatch batch = requireBatch(batchId);

        if (!GroupingStatusEnum.DRAFT.getCode().equals(batch.getGroupingStatus())) {

            throw new ServiceException("仅草稿批次可编辑");

        }

        return batch;

    }



    private BasketDivisionGroup requireGroup(Long groupId, Long batchId) {

        if (groupId == null) {

            throw new ServiceException("groupId 不能为空");

        }

        BasketDivisionGroup group = groupMapper.selectById(groupId);

        if (group == null || !batchId.equals(group.getGroupingBatchId())) {

            throw new ServiceException("小组不存在或不属于该批次");

        }

        return group;

    }



    private BasketDivisionGroupTeam requireAssignment(Long assignmentId, Long batchId) {

        if (assignmentId == null) {

            throw new ServiceException("assignmentId 不能为空");

        }

        BasketDivisionGroupTeam assignment = groupTeamMapper.selectById(assignmentId);

        if (assignment == null || !batchId.equals(assignment.getGroupingBatchId())) {

            throw new ServiceException("分配记录不存在");

        }

        return assignment;

    }



    private BasketDivision requireDivision(Long divisionId) {

        BasketDivision division = divisionMapper.selectById(divisionId);

        if (division == null) {

            throw new ServiceException("Division 不存在");

        }

        return division;

    }



    private BasketDivisionStage requireStage(Long stageId) {

        if (stageId == null) {

            throw new ServiceException("stageId 不能为空");

        }

        BasketDivisionStage stage = stageMapper.selectById(stageId);

        if (stage == null) {

            throw new ServiceException("阶段不存在");

        }

        return stage;

    }

}

