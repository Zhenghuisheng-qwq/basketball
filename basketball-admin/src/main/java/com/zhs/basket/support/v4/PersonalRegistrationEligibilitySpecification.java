package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketPersonalRegistration;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.PersonalRegistrationStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PersonalRegistrationEligibilitySpecification {

    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketTeamMemberMapper teamMemberMapper;

    public void validateForSave(BasketDivision division, Long playerId, BasketPersonalRegistration existing) {
        requirePersonalOpenDivision(division);
        validateRegistrationWindow(division);
        validateCapacity(division);
        participationSupport.requireCertifiedPlayer(playerId);
        participationSupport.requirePlatformIdentity(playerId, division.getPlatformCode());
        assertNoDuplicatePersonal(division.getId(), playerId, existing);
        assertNoTeamRegistrationConflict(division.getId(), playerId);
        assertNoActiveLongTermTeamMembership(playerId);
    }

    public void validateForSubmit(BasketPersonalRegistration registration, BasketDivision division) {
        if (registration == null) {
            throw new ServiceException("个人报名不存在");
        }
        validateForSave(division, registration.getPlayerId(), registration);
        if (!PersonalRegistrationStatusEnum.DRAFT.getCode().equals(registration.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.REJECTED.getCode().equals(registration.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.WITHDRAWN.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("当前状态不可提交");
        }
    }

    public void requirePersonalOpenDivision(BasketDivision division) {
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        if (!RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("该 Division 不支持个人报名");
        }
        if (!DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("该 Division 尚未开放报名");
        }
    }

    private void validateRegistrationWindow(BasketDivision division) {
        Date now = new Date();
        if (division.getRegistrationStartTime() != null && division.getRegistrationStartTime().after(now)) {
            throw new ServiceException("报名尚未开始");
        }
        if (division.getRegistrationEndTime() != null && division.getRegistrationEndTime().before(now)) {
            throw new ServiceException("报名已结束");
        }
    }

    private void validateCapacity(BasketDivision division) {
        if (division.getMaxPersonCount() == null) {
            return;
        }
        int activeCount = personalRegistrationMapper.countActiveByDivisionId(division.getId());
        if (activeCount >= division.getMaxPersonCount()) {
            throw new ServiceException("个人报名名额已满");
        }
    }

    private void assertNoDuplicatePersonal(Long divisionId, Long playerId, BasketPersonalRegistration existing) {
        BasketPersonalRegistration active = personalRegistrationMapper.selectActiveByDivisionPlayer(divisionId, playerId);
        if (active != null && (existing == null || !active.getId().equals(existing.getId()))) {
            throw new ServiceException("您已在该 Division 提交个人报名");
        }
    }

    private void assertNoTeamRegistrationConflict(Long divisionId, Long playerId) {
        if (registrationPlayerMapper.countActivePlayerInDivision(divisionId, playerId, null) > 0) {
            throw new ServiceException("您已通过球队报名参与该 Division，不能重复个人报名");
        }
    }

    private void assertNoActiveLongTermTeamMembership(Long playerId) {
        if (teamMemberMapper.countActiveMembershipByPlayer(playerId) > 0) {
            throw new ServiceException("您已在长期球队中，不能提交个人报名");
        }
    }

    public static String buildActiveUniqueKey(Long divisionId, Long playerId) {
        return "DIVISION:" + divisionId + ":PLAYER:" + playerId;
    }
}
