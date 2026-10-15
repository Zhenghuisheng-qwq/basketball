package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.dto.v4.TeamRosterAllocationReq;
import com.zhs.basket.dto.v4.TeamRosterCandidateOrderReq;
import com.zhs.basket.enums.v4.CandidateMoveDirectionEnum;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.enums.v4.RosterAllocationStrategyTypeEnum;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.ITeamRosterAllocationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.support.v4.CurrentRosterEligibility;
import com.zhs.basket.support.v4.RosterEligibilityQueries;
import com.zhs.basket.support.v4.roster.ParticipationRoleTransition;
import com.zhs.basket.support.v4.roster.RosterAllocationPlan;
import com.zhs.basket.support.v4.roster.RosterAllocationStrategyFactory;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TeamRosterAllocationServiceImpl implements ITeamRosterAllocationService {

    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;
    @Autowired
    private TeamRosterSupport rosterSupport;
    @Autowired
    private RosterAllocationStrategyFactory strategyFactory;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long divisionId, TeamRosterAllocationReq req, Long operatorId) {
        if (req == null || req.getRegistrationId() == null || StringUtils.isEmpty(req.getParticipationRole())) {
            throw new ServiceException("registrationId \u4e0e participationRole \u4e0d\u80fd\u4e3a\u7a7a");
        }
        BasketDivision division = rosterSupport.lockDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        rosterSupport.assertRosterEditable(divisionId);

        BasketRegistration registration = requireRegistration(req.getRegistrationId());
        if (!divisionId.equals(registration.getDivisionId())) {
            throw new ServiceException("\u62a5\u540d\u4e0d\u5c5e\u4e8e\u5f53\u524d Division");
        }
        if (!RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("\u4ec5\u5ba1\u6838\u901a\u8fc7\u7684\u7403\u961f\u53ef\u8fdb\u5165\u6700\u7ec8\u540d\u5355\u5206\u914d");
        }

        ParticipationRoleEnum from = rosterSupport.roleOf(registration);
        ParticipationRoleEnum to = ParticipationRoleEnum.fromCode(req.getParticipationRole());
        if (from == to) {
            return;
        }
        ParticipationRoleTransition.assertCanTransit(from, to);

        if (to == ParticipationRoleEnum.FORMAL) {
            assertCurrentlyEligible(division, registration);
            int formalCount = registrationMapper.countApprovedByDivisionAndRole(
                    divisionId, ParticipationRoleEnum.FORMAL.getCode());
            if (from == ParticipationRoleEnum.FORMAL) {
                formalCount--;
            }
            if (formalCount >= rosterSupport.formalCap(division)) {
                throw new ServiceException("\u6b63\u5f0f\u53c2\u8d5b\u540d\u989d\u5df2\u6ee1\uff0c\u4e0d\u80fd\u518d\u8bbe\u4e3a\u6b63\u5f0f\u53c2\u8d5b");
            }
        }

        applyRole(registration, to);
        if (from == ParticipationRoleEnum.CANDIDATE || to == ParticipationRoleEnum.CANDIDATE) {
            compactCandidates(divisionId);
        }
        writeAudit(divisionId, registration.getId(), "ROSTER_ASSIGN",
                "\u8c03\u6574\u540d\u5355\u89d2\u8272", from.getCode(), to.getCode(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAllocate(Long divisionId, Long operatorId) {
        BasketDivision division = rosterSupport.lockDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        rosterSupport.assertRosterEditable(divisionId);

        List<BasketRegistration> allocatable = new ArrayList<>();
        for (BasketRegistration row : registrationMapper.selectByDivisionId(divisionId)) {
            if (!rosterSupport.isApproved(row)) {
                continue;
            }
            if (rosterSupport.roleOf(row) == ParticipationRoleEnum.EXCLUDED) {
                continue;
            }
            if (!currentlyEligible(division, row)) {
                if (rosterSupport.roleOf(row) == ParticipationRoleEnum.FORMAL) {
                    throw new ServiceException("球队当前有效参赛人数不足，不能进入正式名单分配");
                }
                continue;
            }
            allocatable.add(row);
        }
        RosterAllocationPlan plan = strategyFactory
                .get(RosterAllocationStrategyTypeEnum.AUTO_CAPACITY.getCode())
                .allocate(allocatable, rosterSupport.formalCap(division));
        for (RosterAllocationPlan.Item item : plan.getItems()) {
            BasketRegistration registration = requireRegistration(item.getRegistrationId());
            registration.setParticipationRole(item.getParticipationRole());
            registration.setCandidateOrder(item.getCandidateOrder());
            if (registrationMapper.updateParticipation(registration) != 1) {
                throw new ServiceException("\u81ea\u52a8\u5206\u914d\u66f4\u65b0\u5931\u8d25");
            }
        }
        compactCandidates(divisionId);
        writeAudit(divisionId, divisionId, "ROSTER_AUTO_ALLOCATE",
                "\u6309\u6b63\u5f0f\u540d\u989d\u81ea\u52a8\u5206\u914d", null, RosterAllocationStrategyTypeEnum.AUTO_CAPACITY.getCode(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveCandidate(Long divisionId, TeamRosterCandidateOrderReq req, Long operatorId) {
        if (req == null || req.getRegistrationId() == null) {
            throw new ServiceException("registrationId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        CandidateMoveDirectionEnum direction = CandidateMoveDirectionEnum.fromCode(req.getDirection());
        BasketDivision division = rosterSupport.lockDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        rosterSupport.assertRosterEditable(divisionId);

        BasketRegistration current = requireRegistration(req.getRegistrationId());
        if (!divisionId.equals(current.getDivisionId())) {
            throw new ServiceException("\u62a5\u540d\u4e0d\u5c5e\u4e8e\u5f53\u524d Division");
        }
        if (rosterSupport.roleOf(current) != ParticipationRoleEnum.CANDIDATE) {
            throw new ServiceException("\u4ec5\u5019\u8865\u7403\u961f\u53ef\u4ee5\u8c03\u6574\u5019\u8865\u987a\u5e8f");
        }

        List<BasketRegistration> candidates = listCandidates(divisionId);
        int index = -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (current.getId().equals(candidates.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ServiceException("\u5019\u8865\u7403\u961f\u4e0d\u5b58\u5728");
        }
        int swapIndex = direction == CandidateMoveDirectionEnum.UP ? index - 1 : index + 1;
        if (swapIndex < 0 || swapIndex >= candidates.size()) {
            return;
        }
        BasketRegistration other = candidates.get(swapIndex);
        Integer currentOrder = current.getCandidateOrder();
        Integer otherOrder = other.getCandidateOrder();
        current.setCandidateOrder(otherOrder);
        other.setCandidateOrder(currentOrder);
        if (registrationMapper.updateParticipation(current) != 1
                || registrationMapper.updateParticipation(other) != 1) {
            throw new ServiceException("\u8c03\u6574\u5019\u8865\u987a\u5e8f\u5931\u8d25");
        }
        compactCandidates(divisionId);
        writeAudit(divisionId, current.getId(), "ROSTER_CANDIDATE_ORDER",
                "\u8c03\u6574\u5019\u8865\u987a\u5e8f", String.valueOf(currentOrder), String.valueOf(otherOrder), null);
    }

    private void applyRole(BasketRegistration registration, ParticipationRoleEnum to) {
        registration.setParticipationRole(to.getCode());
        if (to == ParticipationRoleEnum.CANDIDATE) {
            int maxOrder = registrationMapper.selectMaxCandidateOrder(registration.getDivisionId());
            registration.setCandidateOrder(maxOrder + 1);
        } else {
            registration.setCandidateOrder(null);
        }
        if (registrationMapper.updateParticipation(registration) != 1) {
            throw new ServiceException("\u66f4\u65b0\u540d\u5355\u89d2\u8272\u5931\u8d25");
        }
    }

    private void compactCandidates(Long divisionId) {
        List<BasketRegistration> candidates = listCandidates(divisionId);
        int order = 1;
        for (BasketRegistration row : candidates) {
            if (!Objects.equals(row.getCandidateOrder(), order)) {
                row.setCandidateOrder(order);
                if (registrationMapper.updateParticipation(row) != 1) {
                    throw new ServiceException("\u91cd\u6392\u5019\u8865\u987a\u5e8f\u5931\u8d25");
                }
            }
            order++;
        }
    }

    private List<BasketRegistration> listCandidates(Long divisionId) {
        return registrationMapper.selectByDivisionId(divisionId).stream()
                .filter(row -> rosterSupport.roleOf(row) == ParticipationRoleEnum.CANDIDATE)
                .sorted(Comparator
                        .comparing(BasketRegistration::getCandidateOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketRegistration::getId))
                .toList();
    }

    private BasketRegistration requireRegistration(Long registrationId) {
        BasketRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new ServiceException("\u62a5\u540d\u4e0d\u5b58\u5728");
        }
        return registration;
    }

    private void assertCurrentlyEligible(BasketDivision division, BasketRegistration registration) {
        if (!currentlyEligible(division, registration)) {
            throw new ServiceException("球队当前有效参赛人数不足，不能进入正式名单");
        }
    }

    private boolean currentlyEligible(BasketDivision division, BasketRegistration registration) {
        int minimum = CurrentRosterEligibility.minimum(division);
        if (minimum <= 0) {
            return true;
        }
        return eligibilityQueries.eligibleCount(registration.getId(), division.getId()) >= minimum;
    }

    private void writeAudit(Long divisionId, Long objectId, String action, String summary,
                            String before, String after, String reason) {
        List<AuditFieldInput> fields = new ArrayList<>();
        AuditFieldInput field = new AuditFieldInput();
        field.setFieldName("participationRole");
        field.setFieldLabel("\u540d\u5355\u89d2\u8272");
        field.setBeforeValue(before);
        field.setAfterValue(after);
        fields.add(field);
        AuditFieldInput divisionField = new AuditFieldInput();
        divisionField.setFieldName("divisionId");
        divisionField.setFieldLabel("Division");
        divisionField.setAfterValue(String.valueOf(divisionId));
        fields.add(divisionField);
        auditWriter.writeAudit("V4_ROSTER", "DIVISION_ROSTER", objectId, action, summary, reason, fields);
    }
}
