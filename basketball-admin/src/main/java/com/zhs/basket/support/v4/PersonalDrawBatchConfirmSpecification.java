package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AssignmentRoleEnum;
import com.zhs.basket.enums.v4.DrawBatchStatusEnum;
import com.zhs.basket.enums.v4.PersonalRegistrationStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketPersonalDrawAssignmentMapper;
import com.zhs.basket.mapper.v4.BasketPersonalDrawTeamMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PersonalDrawBatchConfirmSpecification {

    @Autowired
    private BasketPersonalDrawTeamMapper drawTeamMapper;
    @Autowired
    private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;

    public void validate(BasketPersonalDrawBatch batch, BasketDivision division, String unassignedPolicy) {
        if (batch == null) {
            throw new ServiceException("抽签批次不存在");
        }
        if (!DrawBatchStatusEnum.DRAFT.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("仅草稿批次可确认");
        }
        if (division == null || !RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("Division 不支持个人抽签");
        }

        List<BasketPersonalDrawTeam> teams = drawTeamMapper.selectByBatchId(batch.getId());
        if (teams.isEmpty()) {
            throw new ServiceException("批次至少需要一个临时队伍");
        }

        List<BasketPersonalDrawAssignment> assignments = drawAssignmentMapper.selectByBatchId(batch.getId());
        Set<Long> assignedPlayerIds = new HashSet<>();
        Set<Long> assignedRegistrationIds = new HashSet<>();

        for (BasketPersonalDrawTeam team : teams) {
            validateTeamSize(division, team);
            List<BasketPersonalDrawAssignment> teamAssignments = assignments.stream()
                    .filter(a -> team.getId().equals(a.getDrawTeamId()))
                    .toList();
            if (teamAssignments.isEmpty()) {
                throw new ServiceException("临时队伍「" + team.getTemporaryTeamName() + "」未分配成员");
            }
            long captainCount = teamAssignments.stream()
                    .filter(a -> AssignmentRoleEnum.CAPTAIN.getCode().equals(a.getAssignmentRole()))
                    .count();
            if (captainCount != 1) {
                throw new ServiceException("临时队伍「" + team.getTemporaryTeamName() + "」必须有且仅有一名队长");
            }
            for (BasketPersonalDrawAssignment assignment : teamAssignments) {
                if (!assignedPlayerIds.add(assignment.getPlayerId())) {
                    throw new ServiceException("球员不能分配到多个临时队伍");
                }
                if (!assignedRegistrationIds.add(assignment.getPersonalRegistrationId())) {
                    throw new ServiceException("个人报名不能重复分配");
                }
                BasketPersonalRegistration reg = personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
                if (reg == null || !PersonalRegistrationStatusEnum.APPROVED.getCode().equals(reg.getRegistrationStatus())) {
                    throw new ServiceException("仅已审核通过的个人报名可参与抽签");
                }
            }
        }

        int approvedCount = personalRegistrationMapper.countApprovedByDivisionId(division.getId());
        int assignedCount = assignedRegistrationIds.size();
        if (assignedCount < approvedCount) {
            if (StringUtils.isEmpty(unassignedPolicy)) {
                throw new ServiceException("存在未分配的个人报名，请设置未分配处理策略");
            }
        }
    }

    private void validateTeamSize(BasketDivision division, BasketPersonalDrawTeam team) {
        int count = team.getActualMemberCount() != null ? team.getActualMemberCount() : 0;
        participationSupport.validateLineupSize(division, count);
    }
}
