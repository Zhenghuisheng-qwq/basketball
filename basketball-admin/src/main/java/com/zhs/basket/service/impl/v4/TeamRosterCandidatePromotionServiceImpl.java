package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.dto.v4.TeamRosterPromoteReq;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.ITeamRosterCandidatePromotionService;
import com.zhs.basket.service.v4.ITeamRosterLockApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.vo.v4.TeamRosterLockResultVo;
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
public class TeamRosterCandidatePromotionServiceImpl implements ITeamRosterCandidatePromotionService {

    @Autowired
    private TeamRosterSupport rosterSupport;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private ITeamRosterLockApplicationService lockApplicationService;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRosterLockResultVo promote(Long divisionId, TeamRosterPromoteReq req, Long operatorId) {
        if (req == null || req.getFormalRegistrationId() == null || req.getCandidateRegistrationId() == null) {
            throw new ServiceException("\u6b63\u5f0f\u7403\u961f\u4e0e\u5019\u8865\u7403\u961f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (StringUtils.isEmpty(req.getReason()) || req.getReason().trim().isEmpty()) {
            throw new ServiceException("\u5019\u8865\u9012\u8865\u5fc5\u987b\u586b\u5199\u539f\u56e0");
        }
        if (req.getFormalRegistrationId().equals(req.getCandidateRegistrationId())) {
            throw new ServiceException("\u6b63\u5f0f\u7403\u961f\u4e0e\u5019\u8865\u7403\u961f\u4e0d\u80fd\u76f8\u540c");
        }

        BasketDivision division = rosterSupport.lockDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        if (current == null) {
            throw new ServiceException("\u5c1a\u672a\u9501\u5b9a\u6700\u7ec8\u540d\u5355\uff0c\u8bf7\u76f4\u63a5\u8c03\u6574\u5206\u914d\u540e\u9501\u5b9a");
        }
        if (!matchMapper.selectFinishedPublishedByDivisionId(divisionId).isEmpty()) {
            throw new ServiceException("\u8be5\u8d5b\u4e8b\u5df2\u5b58\u5728\u6b63\u5f0f\u6bd4\u8d5b\u7ed3\u679c\uff0c\u4e0d\u80fd\u901a\u8fc7\u540d\u5355\u9012\u8865\u76f4\u63a5\u66ff\u6362\u5386\u53f2\u53c2\u8d5b\u7403\u961f\u3002\u8bf7\u4f7f\u7528\u9000\u8d5b / \u5224\u8d1f / Forfeit \u7b49\u8d5b\u4e8b\u5f02\u5e38\u6d41\u7a0b");
        }

        BasketRegistration formal = requireInDivision(req.getFormalRegistrationId(), divisionId);
        BasketRegistration candidate = requireInDivision(req.getCandidateRegistrationId(), divisionId);
        if (rosterSupport.roleOf(formal) != ParticipationRoleEnum.FORMAL) {
            throw new ServiceException("\u88ab\u66ff\u6362\u7403\u961f\u5f53\u524d\u4e0d\u662f\u6b63\u5f0f\u53c2\u8d5b");
        }
        if (rosterSupport.roleOf(candidate) != ParticipationRoleEnum.CANDIDATE) {
            throw new ServiceException("\u9012\u8865\u7403\u961f\u5f53\u524d\u4e0d\u662f\u5019\u8865");
        }

        formal.setParticipationRole(ParticipationRoleEnum.EXCLUDED.getCode());
        formal.setCandidateOrder(null);
        candidate.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
        candidate.setCandidateOrder(null);
        if (registrationMapper.updateParticipation(formal) != 1
                || registrationMapper.updateParticipation(candidate) != 1) {
            throw new ServiceException("\u5019\u8865\u9012\u8865\u66f4\u65b0\u5931\u8d25");
        }
        compactCandidates(divisionId);

        List<AuditFieldInput> fields = new ArrayList<>();
        addField(fields, "formalRegistrationId", "\u539f\u6b63\u5f0f\u7403\u961f", String.valueOf(formal.getId()),
                ParticipationRoleEnum.EXCLUDED.getCode());
        addField(fields, "candidateRegistrationId", "\u9012\u8865\u7403\u961f", String.valueOf(candidate.getId()),
                ParticipationRoleEnum.FORMAL.getCode());
        addField(fields, "previousRosterVersionId", "\u539f\u540d\u5355\u7248\u672c", String.valueOf(current.getId()), null);
        auditWriter.writeAudit("V4_ROSTER", "DIVISION_ROSTER", divisionId, "ROSTER_PROMOTE_CANDIDATE",
                "\u5019\u8865\u9012\u8865", req.getReason().trim(), fields);

        return lockApplicationService.lock(divisionId, operatorId);
    }

    private BasketRegistration requireInDivision(Long registrationId, Long divisionId) {
        BasketRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new ServiceException("\u62a5\u540d\u4e0d\u5b58\u5728");
        }
        if (!divisionId.equals(registration.getDivisionId())) {
            throw new ServiceException("\u62a5\u540d\u4e0d\u5c5e\u4e8e\u5f53\u524d Division");
        }
        if (!rosterSupport.isApproved(registration)) {
            throw new ServiceException("\u9012\u8865\u53cc\u65b9\u5fc5\u987b\u662f\u5ba1\u6838\u901a\u8fc7\u7684\u62a5\u540d");
        }
        return registration;
    }

    private void compactCandidates(Long divisionId) {
        List<BasketRegistration> candidates = registrationMapper.selectByDivisionId(divisionId).stream()
                .filter(row -> rosterSupport.roleOf(row) == ParticipationRoleEnum.CANDIDATE)
                .sorted(Comparator
                        .comparing(BasketRegistration::getCandidateOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketRegistration::getId))
                .toList();
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

    private void addField(List<AuditFieldInput> fields, String name, String label, String before, String after) {
        AuditFieldInput field = new AuditFieldInput();
        field.setFieldName(name);
        field.setFieldLabel(label);
        field.setBeforeValue(before);
        field.setAfterValue(after);
        fields.add(field);
    }
}
