package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Division 规则是否允许修改/重建。
 */
@Component
public class DivisionRuleMutableSpecification {

    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired(required = false)
    private BasketPersonalRegistrationMapper personalRegistrationMapper;

    public void assertCanRebuild(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        assertCanEditRules(division, snapshot);
    }

    public void assertCanEditRules(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (!DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("仅草稿状态 Division 可编辑规则");
        }
        if (snapshot == null) {
            throw new ServiceException("Division 缺少规则快照");
        }
        if (!SnapshotStatusEnum.DRAFT.getCode().equals(snapshot.getSnapshotStatus())) {
            throw new ServiceException("仅草稿状态规则快照可编辑");
        }
        if (snapshot.getIsCurrent() != null && snapshot.getIsCurrent() != 1) {
            throw new ServiceException("仅当前快照可编辑");
        }
        assertNoBusinessData(division.getId());
    }

    public void assertCanChangeKeyFields(BasketDivision division) {
        if (DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())
                || DivisionStatusEnum.REGISTRATION_CLOSED.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("报名已开放后不允许修改平台、比赛模式、报名方式或规则模板");
        }
        if (hasActiveRegistration(division.getId()) || hasEventTeam(division.getId())) {
            throw new ServiceException("已有有效报名或参赛快照，不允许修改关键规则字段");
        }
    }

    public void assertCanChangeGameMode(BasketDivision division) {
        assertCanChangeKeyFields(division);
    }

    public void assertNoBusinessData(Long divisionId) {
        if (hasActiveRegistration(divisionId)) {
            throw new ServiceException("已有有效报名，不能修改规则");
        }
        if (hasPersonalRegistration(divisionId)) {
            throw new ServiceException("已有个人报名，不能修改规则");
        }
        if (hasEventTeam(divisionId)) {
            throw new ServiceException("已有参赛球队快照，不能修改规则");
        }
        if (scheduleBatchMapper.countByDivisionId(divisionId) > 0) {
            throw new ServiceException("已有赛程批次，不能修改规则");
        }
        if (matchMapper.countByDivisionId(divisionId) > 0) {
            throw new ServiceException("已有比赛，不能修改规则");
        }
    }

    public boolean hasActiveRegistration(Long divisionId) {
        return registrationMapper.countActiveByDivisionId(divisionId) > 0;
    }

    public boolean hasPersonalRegistration(Long divisionId) {
        if (personalRegistrationMapper == null) {
            return false;
        }
        return personalRegistrationMapper.countActiveByDivisionId(divisionId) > 0;
    }

    public boolean hasEventTeam(Long divisionId) {
        return eventTeamMapper.countActiveByDivisionId(divisionId) > 0;
    }

    public boolean isMutable(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (division == null || snapshot == null) {
            return false;
        }
        if (!DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus())) {
            return false;
        }
        if (!SnapshotStatusEnum.DRAFT.getCode().equals(snapshot.getSnapshotStatus())) {
            return false;
        }
        if (snapshot.getIsCurrent() != null && snapshot.getIsCurrent() != 1) {
            return false;
        }
        try {
            assertNoBusinessData(division.getId());
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }
}
