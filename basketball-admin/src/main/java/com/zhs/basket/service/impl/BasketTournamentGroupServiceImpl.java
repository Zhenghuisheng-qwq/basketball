package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentGroup;
import com.zhs.basket.domain.BasketTournamentGroupTeam;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.TournamentGroupStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketTournamentGroupMapper;
import com.zhs.basket.mapper.BasketTournamentGroupTeamMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.req.GroupCreateReq;
import com.zhs.basket.req.GroupTeamAssignReq;
import com.zhs.basket.req.GroupUpdateReq;
import com.zhs.basket.service.IBasketTournamentGroupService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.AvailableTeamVo;
import com.zhs.basket.vo.GroupDetailVo;
import com.zhs.basket.vo.GroupListVo;
import com.zhs.basket.vo.GroupTeamVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketTournamentGroupServiceImpl implements IBasketTournamentGroupService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer GROUP_TEAM_ACTIVE = 1;

    @Autowired private BasketTournamentGroupMapper groupMapper;
    @Autowired private BasketTournamentGroupTeamMapper groupTeamMapper;
    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<GroupListVo> selectByStageId(Long stageId) {
        requireStage(stageId);
        return groupMapper.selectByStageId(stageId).stream().map(this::enrichListVo).collect(Collectors.toList());
    }

    @Override
    public GroupDetailVo selectById(Long groupId) {
        BasketTournamentGroup group = requireGroup(groupId);
        return toDetailVo(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long stageId, GroupCreateReq req) {
        BasketTournamentStage stage = requireStage(stageId);
        validateGroupNameUnique(stageId, req.getGroupName(), null);
        BasketTournamentGroup group = new BasketTournamentGroup();
        BeanUtils.copyBeanProp(group, req);
        group.setSeasonId(stage.getSeasonId());
        group.setStageId(stageId);
        group.setGroupCode(playerNoGenerator.generateBizNo("GP"));
        group.setIsDelete(NOT_DELETED);
        if (group.getSequenceNo() == null) {
            group.setSequenceNo(0);
        }
        if (group.getQualifyCount() == null) {
            group.setQualifyCount(0);
        }
        if (group.getGroupStatus() == null) {
            group.setGroupStatus(TournamentGroupStatusEnum.ACTIVE.getCode());
        }
        // 数据库无 maxTeamCount 字段，不做分组容量上限校验
        groupMapper.insert(group);
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long groupId, GroupUpdateReq req) {
        BasketTournamentGroup group = requireGroup(groupId);
        validateGroupNameUnique(group.getStageId(), req.getGroupName(), groupId);
        BeanUtils.copyBeanProp(group, req);
        if (groupMapper.updateById(group) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long groupId) {
        requireGroup(groupId);
        if (matchMapper.countByGroupId(groupId) > 0) {
            throw new ServiceException("分组下存在比赛，无法删除");
        }
        groupTeamMapper.softDeleteByGroupId(groupId);
        if (groupMapper.softDelete(groupId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    @Override
    public List<GroupTeamVo> selectTeamsByGroupId(Long groupId) {
        requireGroup(groupId);
        return groupTeamMapper.selectByGroupId(groupId);
    }

    @Override
    public List<AvailableTeamVo> selectAvailableTeams(Long groupId) {
        BasketTournamentGroup group = requireGroup(groupId);
        return groupTeamMapper.selectAvailableTeamsByStageId(group.getSeasonId(), group.getStageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTeams(Long groupId, GroupTeamAssignReq req) {
        BasketTournamentGroup group = requireGroup(groupId);
        Long[] teamIds = req.getTeamIds() != null ? req.getTeamIds() : new Long[0];
        Set<Long> newTeamIdSet = new HashSet<>(Arrays.asList(teamIds));

        List<GroupTeamVo> currentTeams = groupTeamMapper.selectByGroupId(groupId);
        for (GroupTeamVo current : currentTeams) {
            if (!newTeamIdSet.contains(current.getTeamId())
                    && matchMapper.countByGroupAndTeam(groupId, current.getTeamId()) > 0) {
                throw new ServiceException("球队已参与本分组比赛，无法移除");
            }
        }

        List<AvailableTeamVo> approvedTeams = registrationMapper.selectApprovedTeamsBySeasonId(group.getSeasonId());
        Set<Long> approvedTeamIds = approvedTeams.stream().map(AvailableTeamVo::getTeamId).collect(Collectors.toSet());
        for (Long teamId : newTeamIdSet) {
            if (!approvedTeamIds.contains(teamId)) {
                throw new ServiceException("球队未通过本赛季报名审核");
            }
            if (groupTeamMapper.countByStageIdAndTeamId(group.getStageId(), teamId, groupId) > 0) {
                throw new ServiceException("同阶段下球队已分配至其他分组");
            }
        }

        groupTeamMapper.softDeleteByGroupId(groupId);
        if (!newTeamIdSet.isEmpty()) {
            List<BasketTournamentGroupTeam> toInsert = new ArrayList<>();
            for (Long teamId : newTeamIdSet) {
                BasketTournamentGroupTeam gt = new BasketTournamentGroupTeam();
                gt.setSeasonId(group.getSeasonId());
                gt.setStageId(group.getStageId());
                gt.setGroupId(groupId);
                gt.setTeamId(teamId);
                gt.setGroupTeamStatus(GROUP_TEAM_ACTIVE);
                gt.setIsDelete(NOT_DELETED);
                toInsert.add(gt);
            }
            groupTeamMapper.batchInsert(toInsert);
        }
    }

    private void validateGroupNameUnique(Long stageId, String groupName, Long excludeId) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new ServiceException("分组名称不能为空");
        }
        if (groupMapper.countByStageIdAndGroupName(stageId, groupName.trim(), excludeId) > 0) {
            throw new ServiceException("同阶段下分组名称已存在");
        }
    }

    private BasketTournamentStage requireStage(Long id) {
        BasketTournamentStage stage = stageMapper.selectById(id);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private BasketTournamentGroup requireGroup(Long id) {
        BasketTournamentGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new ServiceException("分组不存在");
        }
        return group;
    }

    private GroupListVo enrichListVo(GroupListVo vo) {
        vo.setGroupStatusName(TournamentGroupStatusEnum.fromCode(vo.getGroupStatus()).getDescription());
        return vo;
    }

    private GroupDetailVo toDetailVo(BasketTournamentGroup group) {
        GroupDetailVo vo = new GroupDetailVo();
        BeanUtils.copyBeanProp(vo, group);
        vo.setGroupStatusName(TournamentGroupStatusEnum.fromCode(group.getGroupStatus()).getDescription());
        BasketTournamentStage stage = stageMapper.selectById(group.getStageId());
        if (stage != null) {
            vo.setStageName(stage.getStageName());
        }
        return vo;
    }
}
