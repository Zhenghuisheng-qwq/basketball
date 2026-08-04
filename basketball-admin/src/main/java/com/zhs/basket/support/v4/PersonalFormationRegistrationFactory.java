package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.AssignmentRoleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RegistrationSourceEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketPersonalDrawAssignmentMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class PersonalFormationRegistrationFactory {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;

    public FormationRegistrationBundle build(BasketPersonalDrawTeam drawTeam,
                                               BasketDivision division,
                                               BasketPersonalDrawBatch batch) {
        if (drawTeam.getLongTermTeamId() == null) {
            throw new ServiceException("未绑定长期球队");
        }
        List<BasketPersonalDrawAssignment> assignments = drawAssignmentMapper.selectByTeamId(drawTeam.getId());
        if (assignments.isEmpty()) {
            throw new ServiceException("临时队伍无成员");
        }

        Long captainPlayerId = drawTeam.getCaptainPlayerId();
        Long captainMemberId = null;
        List<Long> playerIds = new ArrayList<>();
        for (BasketPersonalDrawAssignment assignment : assignments) {
            playerIds.add(assignment.getPlayerId());
            if (AssignmentRoleEnum.CAPTAIN.getCode().equals(assignment.getAssignmentRole())) {
                captainPlayerId = assignment.getPlayerId();
            }
        }
        if (captainPlayerId == null) {
            throw new ServiceException("未找到队长");
        }

        BasketPersonalRegistration captainReg = findRegistrationByPlayer(assignments, captainPlayerId);
        if (captainReg != null) {
            captainMemberId = findMemberId(captainReg.getPlayerId());
        }
        if (captainMemberId == null && captainReg != null) {
            captainMemberId = participationSupport.requireCertifiedPlayer(captainPlayerId).getMemberId();
        } else if (captainMemberId == null) {
            captainMemberId = participationSupport.requireCertifiedPlayer(captainPlayerId).getMemberId();
        }

        Date now = new Date();
        BasketRegistration registration = new BasketRegistration();
        registration.setSeasonId(division.getSeasonId());
        registration.setDivisionId(division.getId());
        registration.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        registration.setRegistrationSource(RegistrationSourceEnum.PERSONAL_FORMATION.getCode());
        registration.setSourcePersonalDrawTeamId(drawTeam.getId());
        registration.setSourcePersonalDrawBatchId(batch.getId());
        registration.setTeamId(drawTeam.getLongTermTeamId());
        registration.setCaptainPlayerId(captainPlayerId);
        registration.setCaptainMemberId(captainMemberId);
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        registration.setLineupCount(playerIds.size());
        registration.setSubmitTime(now);
        registration.setApproveTime(now);
        registration.setCreateTime(now);
        registration.setUpdateTime(now);
        registration.setIsDelete(NOT_DELETED);

        List<BasketRegistrationPlayer> players = participationSupport.buildRegistrationPlayers(
                division, null, drawTeam.getLongTermTeamId(), captainPlayerId, playerIds);

        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setDivisionId(division.getId());
        audit.setSeasonId(division.getSeasonId());
        audit.setAuditAction("APPROVE");
        audit.setAuditResult("APPROVED");
        audit.setBeforeStatus(RegistrationStatusEnum.PENDING_REVIEW.getCode());
        audit.setAfterStatus(RegistrationStatusEnum.APPROVED.getCode());
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);

        FormationRegistrationBundle bundle = new FormationRegistrationBundle();
        bundle.setRegistration(registration);
        bundle.setPlayers(players);
        bundle.setAudit(audit);
        bundle.setAssignments(assignments);
        return bundle;
    }

    private BasketPersonalRegistration findRegistrationByPlayer(List<BasketPersonalDrawAssignment> assignments, Long playerId) {
        for (BasketPersonalDrawAssignment assignment : assignments) {
            if (playerId.equals(assignment.getPlayerId())) {
                return personalRegistrationMapper.selectById(assignment.getPersonalRegistrationId());
            }
        }
        return null;
    }

    private Long findMemberId(Long playerId) {
        return participationSupport.requireCertifiedPlayer(playerId).getMemberId();
    }

    @lombok.Data
    public static class FormationRegistrationBundle {
        private BasketRegistration registration;
        private List<BasketRegistrationPlayer> players;
        private BasketRegistrationAudit audit;
        private List<BasketPersonalDrawAssignment> assignments;
    }
}
