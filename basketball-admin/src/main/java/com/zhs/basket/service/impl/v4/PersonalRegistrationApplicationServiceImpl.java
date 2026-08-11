package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketPersonalRegistration;
import com.zhs.basket.dto.v4.BasketPersonalRegistrationRejectReq;
import com.zhs.basket.enums.v4.PersonalAuditActionEnum;
import com.zhs.basket.enums.v4.PersonalRegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.service.v4.IPersonalRegistrationApplicationService;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.basket.support.v4.PersonalRegistrationEligibilitySpecification;
import com.zhs.basket.support.v4.PersonalRegistrationStatusTransition;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationDetailVo;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonalRegistrationApplicationServiceImpl implements IPersonalRegistrationApplicationService {

    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketPersonalRegistrationAuditMapper personalRegistrationAuditMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private PersonalRegistrationEligibilitySpecification eligibilitySpecification;
    @Autowired
    private PersonalRegistrationStatusTransition statusTransition;
    @Autowired
    private BasketAdminMessageService messageService;

    @Override
    public List<BasketPersonalRegistrationListVo> selectList(BasketPersonalRegistrationQuery query) {
        return personalRegistrationMapper.selectVoList(query);
    }

    @Override
    public BasketPersonalRegistrationDetailVo selectDetail(Long personalRegistrationId) {
        BasketPersonalRegistration registration = requireRegistration(personalRegistrationId);
        BasketPersonalRegistrationDetailVo vo = new BasketPersonalRegistrationDetailVo();
        vo.setPersonalRegistrationId(registration.getId());
        vo.setSeasonId(registration.getSeasonId());
        vo.setDivisionId(registration.getDivisionId());
        vo.setPlayerId(registration.getPlayerId());
        vo.setActualPlatformCode(registration.getActualPlatformCode());
        vo.setGameAccountId(registration.getGameAccountId());
        vo.setPreferredPosition(registration.getPreferredPosition());
        vo.setRegistrationStatus(registration.getRegistrationStatus());
        vo.setSubmitTime(registration.getSubmitTime());
        vo.setApprovedTime(registration.getApprovedTime());
        vo.setRejectReason(registration.getRejectReason());
        vo.setAssignedDrawTeamId(registration.getAssignedDrawTeamId());
        vo.setConvertedRegistrationId(registration.getConvertedRegistrationId());
        vo.setAudits(personalRegistrationAuditMapper.selectVoByPersonalRegistrationId(personalRegistrationId));

        BasketPersonalRegistrationQuery query = new BasketPersonalRegistrationQuery();
        query.setDivisionId(registration.getDivisionId());
        personalRegistrationMapper.selectVoList(query).stream()
                .filter(row -> personalRegistrationId.equals(row.getPersonalRegistrationId()))
                .findFirst()
                .ifPresent(row -> {
                    vo.setSeasonTitle(row.getSeasonTitle());
                    vo.setDivisionName(row.getDivisionName());
                    vo.setPlatformCode(row.getPlatformCode());
                    vo.setPlayerName(row.getPlayerName());
                });
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long personalRegistrationId) {
        BasketPersonalRegistration registration = requireRegistration(personalRegistrationId);
        if (!PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可通过");
        }
        BasketDivision division = requireDivision(registration.getDivisionId());
        eligibilitySpecification.validateForSave(division, registration.getPlayerId(), registration);

        registration.setActiveUniqueKey(
                PersonalRegistrationEligibilitySpecification.buildActiveUniqueKey(division.getId(), registration.getPlayerId()));
        registration.setRejectReason(null);
        statusTransition.transition(registration, PersonalRegistrationStatusEnum.APPROVED,
                PersonalAuditActionEnum.APPROVE, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long personalRegistrationId, BasketPersonalRegistrationRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketPersonalRegistration registration = requireRegistration(personalRegistrationId);
        if (!PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可驳回");
        }
        String rejectReason = req.getRejectReason().trim();
        registration.setRejectReason(rejectReason);
        statusTransition.transition(registration, PersonalRegistrationStatusEnum.REJECTED,
                PersonalAuditActionEnum.REJECT, req.getAuditRemark());
        notifyPersonalRegistrationRejected(registration, rejectReason);
    }

    private void notifyPersonalRegistrationRejected(BasketPersonalRegistration registration, String rejectReason) {
        Long memberId = registration.getUserId();
        if (memberId == null) {
            return;
        }
        BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
        String divisionLabel = division != null && StringUtils.isNotEmpty(division.getDivisionName())
                ? division.getDivisionName() : "当前赛区";
        messageService.sendSiteMessage(
                memberId,
                SecurityUtils.getUserId(),
                "PERSONAL_REGISTRATION",
                registration.getId(),
                "个人报名已驳回",
                "您在「" + divisionLabel + "」的个人报名未通过审核，原因：" + rejectReason);
    }

    private BasketPersonalRegistration requireRegistration(Long id) {
        if (id == null) {
            throw new ServiceException("personalRegistrationId 不能为空");
        }
        BasketPersonalRegistration registration = personalRegistrationMapper.selectById(id);
        if (registration == null) {
            throw new ServiceException("个人报名不存在");
        }
        return registration;
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }
}
