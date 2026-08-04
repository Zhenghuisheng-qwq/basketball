package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketPersonalRegistration;
import com.zhs.basket.domain.v4.BasketPersonalRegistrationAudit;
import com.zhs.basket.enums.v4.PersonalAuditActionEnum;
import com.zhs.basket.enums.v4.PersonalRegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PersonalRegistrationStatusTransition {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketPersonalRegistrationAuditMapper personalRegistrationAuditMapper;

    public void transition(BasketPersonalRegistration registration,
                           PersonalRegistrationStatusEnum targetStatus,
                           PersonalAuditActionEnum action,
                           String remark) {
        if (registration == null) {
            throw new ServiceException("个人报名不存在");
        }
        String beforeStatus = registration.getRegistrationStatus();
        Date now = new Date();
        registration.setRegistrationStatus(targetStatus.getCode());
        registration.setUpdateTime(now);
        registration.setUpdateBy(SecurityUtils.getUserId());

        switch (targetStatus) {
            case PENDING_REVIEW -> registration.setSubmitTime(now);
            case APPROVED -> {
                registration.setApprovedTime(now);
                registration.setRejectReason(null);
            }
            case REJECTED, WITHDRAWN -> registration.setActiveUniqueKey(null);
            case ASSIGNED -> { /* assignedDrawTeamId set by caller */ }
            case CONVERTED -> { /* convertedRegistrationId set by caller */ }
            default -> { }
        }

        if (personalRegistrationMapper.updateById(registration) != 1) {
            throw new ServiceException("个人报名状态更新失败");
        }
        if (targetStatus == PersonalRegistrationStatusEnum.REJECTED
                || targetStatus == PersonalRegistrationStatusEnum.WITHDRAWN) {
            personalRegistrationMapper.updateActiveUniqueKey(registration.getId(), null);
        }
        writeAudit(registration, action, beforeStatus, targetStatus.getCode(), remark);
    }

    public void transitionByUser(BasketPersonalRegistration registration,
                                 PersonalRegistrationStatusEnum targetStatus,
                                 PersonalAuditActionEnum action,
                                 Long operatorId,
                                 String remark) {
        if (registration == null) {
            throw new ServiceException("个人报名不存在");
        }
        String beforeStatus = registration.getRegistrationStatus();
        Date now = new Date();
        registration.setRegistrationStatus(targetStatus.getCode());
        registration.setUpdateTime(now);
        registration.setUpdateBy(operatorId);

        switch (targetStatus) {
            case PENDING_REVIEW -> registration.setSubmitTime(now);
            case APPROVED -> {
                registration.setApprovedTime(now);
                registration.setRejectReason(null);
            }
            case REJECTED, WITHDRAWN -> registration.setActiveUniqueKey(null);
            default -> { }
        }

        if (personalRegistrationMapper.updateById(registration) != 1) {
            throw new ServiceException("个人报名状态更新失败");
        }
        if (targetStatus == PersonalRegistrationStatusEnum.REJECTED
                || targetStatus == PersonalRegistrationStatusEnum.WITHDRAWN) {
            personalRegistrationMapper.updateActiveUniqueKey(registration.getId(), null);
        }

        BasketPersonalRegistrationAudit audit = new BasketPersonalRegistrationAudit();
        audit.setPersonalRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(targetStatus.getCode());
        audit.setAuditAction(action.getCode());
        audit.setAuditRemark(remark);
        audit.setOperatorId(operatorId);
        audit.setOperateTime(now);
        audit.setCreateTime(now);
        audit.setIsDelete(NOT_DELETED);
        personalRegistrationAuditMapper.insert(audit);
    }

    private void writeAudit(BasketPersonalRegistration registration,
                            PersonalAuditActionEnum action,
                            String beforeStatus,
                            String afterStatus,
                            String remark) {
        Date now = new Date();
        BasketPersonalRegistrationAudit audit = new BasketPersonalRegistrationAudit();
        audit.setPersonalRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditAction(action.getCode());
        audit.setAuditRemark(remark);
        audit.setOperatorId(SecurityUtils.getUserId());
        audit.setOperateTime(now);
        audit.setCreateTime(now);
        audit.setIsDelete(NOT_DELETED);
        personalRegistrationAuditMapper.insert(audit);
    }
}
