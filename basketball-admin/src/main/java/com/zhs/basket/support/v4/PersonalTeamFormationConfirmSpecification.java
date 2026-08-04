package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AssignmentRoleEnum;
import com.zhs.basket.enums.v4.DrawBatchStatusEnum;
import com.zhs.basket.enums.v4.FormationStatusEnum;
import com.zhs.basket.enums.v4.JoinStatusEnum;
import com.zhs.basket.mapper.v4.BasketPersonalDrawAssignmentMapper;
import com.zhs.basket.mapper.v4.BasketPersonalDrawBatchMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersonalTeamFormationConfirmSpecification {

    @Autowired
    private BasketPersonalDrawBatchMapper drawBatchMapper;
    @Autowired
    private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;

    public void validate(BasketPersonalDrawTeam drawTeam, BasketDivision division) {
        if (drawTeam == null) {
            throw new ServiceException("临时队伍不存在");
        }
        if (drawTeam.getConvertedRegistrationId() != null) {
            return;
        }
        if (drawTeam.getLongTermTeamId() == null) {
            throw new ServiceException("请先绑定长期球队");
        }
        if (!FormationStatusEnum.READY_TO_CONFIRM.getCode().equals(drawTeam.getFormationStatus())
                && !FormationStatusEnum.CONFIRMED.getCode().equals(drawTeam.getFormationStatus())) {
            throw new ServiceException("临时队伍尚未进入可确认状态");
        }

        BasketPersonalDrawBatch batch = drawBatchMapper.selectById(drawTeam.getDrawBatchId());
        if (batch == null || !DrawBatchStatusEnum.CONFIRMED.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("抽签批次尚未确认");
        }

        participationSupport.requireValidTeam(drawTeam.getLongTermTeamId());
        if (drawTeam.getCaptainPlayerId() != null) {
            participationSupport.requireTeamMember(drawTeam.getLongTermTeamId(), drawTeam.getCaptainPlayerId());
        }

        List<BasketPersonalDrawAssignment> assignments = drawAssignmentMapper.selectByTeamId(drawTeam.getId());
        if (assignments.isEmpty()) {
            throw new ServiceException("临时队伍无成员分配");
        }
        long captainCount = assignments.stream()
                .filter(a -> AssignmentRoleEnum.CAPTAIN.getCode().equals(a.getAssignmentRole()))
                .count();
        if (captainCount != 1) {
            throw new ServiceException("临时队伍必须有且仅有一名队长");
        }

        for (BasketPersonalDrawAssignment assignment : assignments) {
            String joinStatus = assignment.getJoinStatus();
            if (!JoinStatusEnum.JOINED.getCode().equals(joinStatus)
                    && !JoinStatusEnum.CONFIRMED.getCode().equals(joinStatus)) {
                throw new ServiceException("仍有成员未完成加入确认");
            }
            participationSupport.requireTeamMember(drawTeam.getLongTermTeamId(), assignment.getPlayerId());
        }

        participationSupport.validateLineupSize(division, assignments.size());
    }
}
