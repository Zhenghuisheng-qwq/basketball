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
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired(required = false)
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Autowired
    private PublishedFormatRevisionGuard publishedFormatRevisionGuard;

    public void assertCanRebuild(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (snapshot == null) {
            if (division == null || !DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus())) {
                throw new ServiceException("仅草稿状态 Division 可编辑规则");
            }
            assertNoBusinessData(division.getId());
            return;
        }
        assertCanEditRules(division, snapshot);
    }

    public boolean superAdminMayRewriteUnstarted(BasketDivision division) {
        return com.zhs.common.utils.SecurityUtils.isAdmin()
                && publishedFormatRevisionGuard.unstartedRewriteBlockers(division).isEmpty();
    }

    public void assertCanEditRules(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (superAdminMayRewriteUnstarted(division)) {
            return;
        }
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
        assertCurrentSnapshotNotLocked(division);
        if (DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())
                || DivisionStatusEnum.REGISTRATION_CLOSED.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("报名已开放后不允许修改平台、比赛模式、报名方式或规则模板");
        }
        if (hasActiveRegistration(division.getId()) || hasEventTeam(division.getId())) {
            throw new ServiceException("已有有效报名或参赛快照，不允许修改关键规则字段");
        }
    }

    public void assertCurrentSnapshotNotLocked(BasketDivision division) {
        if (division == null || division.getId() == null) {
            return;
        }
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(division.getId());
        if (snapshot != null && SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond()) {
            throw new ServiceException("规则已锁定，不允许修改平台、比赛模式、报名方式或规则模板");
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

    /**
     * Division 更新里的 formatTemplateId 不能覆盖当前 Rule Snapshot。
     * 已有当前快照时，指针必须跟着 Snapshot，避免向导脏模板 ID 重建旧 CUSTOM。
     */
    public Long resolveFormatTemplateIdForUpdate(Long requestedTemplateId, BasketDivisionFormatSnapshot current) {
        if (current != null && current.getSourceTemplateId() != null) {
            return current.getSourceTemplateId();
        }
        return requestedTemplateId;
    }

    public boolean canApplyStandardTemplate(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (snapshot != null && SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond()) {
            return false;
        }
        if (snapshot == null) {
            return division != null && DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus());
        }
        return isMutable(division, snapshot);
    }

    public void assertCanApplyStandardTemplate(BasketDivision division, BasketDivisionFormatSnapshot snapshot) {
        if (superAdminMayRewriteUnstarted(division)) {
            return;
        }
        if (snapshot != null && SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond()) {
            throw new ServiceException("已锁定规则快照不允许应用标准模板");
        }
        assertCanRebuild(division, snapshot);
    }
}
