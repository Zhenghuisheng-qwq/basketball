package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RosterVersionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class BasketV4RosterVersionServiceImpl implements IBasketV4RosterVersionService {

    public static String buildActiveUniqueKey(Long divisionId) {
        return "ROSTER:" + divisionId;
    }

    @Autowired
    private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketDivisionRosterVersion createVersion(Long divisionId, Long operatorId) {
        BasketDivision division = lockDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        if (snapshot == null) {
            throw new ServiceException("Division 缺少规则快照，无法创建名单版本");
        }
        RegistrationModeEnum.fromCode(division.getRegistrationMode());

        Date now = new Date();
        BasketDivisionRosterVersion version = new BasketDivisionRosterVersion();
        version.setDivisionId(divisionId);
        version.setVersionNo(rosterVersionMapper.selectMaxVersionByDivisionId(divisionId) + 1);
        version.setRegistrationMode(division.getRegistrationMode());
        version.setRuleVersionId(snapshot.getId());
        version.setStatus(RosterVersionStatusEnum.DRAFT.getCode());
        version.setIsCurrent(0);
        version.setActiveUniqueKey(null);
        version.setFormalCount(0);
        version.setCandidateCount(0);
        version.setExcludedCount(0);
        version.setCreateTime(now);
        version.setUpdateTime(now);
        version.setIsDelete(0);
        try {
            if (rosterVersionMapper.insert(version) != 1) {
                throw new ServiceException("创建名单版本失败");
            }
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ServiceException("名单版本创建冲突，请重试");
        }
        return version;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketDivisionRosterVersion lockVersion(Long rosterVersionId, Long operatorId) {
        BasketDivisionRosterVersion version = requireVersion(rosterVersionId);
        if (RosterVersionStatusEnum.LOCKED.getCode().equals(version.getStatus())
                && version.getIsCurrent() != null && version.getIsCurrent() == 1) {
            return version;
        }
        if (!RosterVersionStatusEnum.DRAFT.getCode().equals(version.getStatus())) {
            throw new ServiceException("仅草稿名单版本可锁定");
        }

        BasketDivisionRosterVersion current = rosterVersionMapper.selectCurrentLockedByDivisionId(version.getDivisionId());
        if (current != null && !current.getId().equals(version.getId())) {
            rosterVersionMapper.clearActiveUniqueKey(current.getId());
            if (rosterVersionMapper.markSuperseded(current.getId()) != 1) {
                throw new ServiceException("失效旧名单版本失败");
            }
        }

        Date now = new Date();
        version.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        version.setIsCurrent(1);
        version.setActiveUniqueKey(buildActiveUniqueKey(version.getDivisionId()));
        version.setLockedTime(now);
        version.setLockedBy(operatorId);
        version.setUpdateTime(now);
        if (rosterVersionMapper.updateById(version) != 1) {
            throw new ServiceException("锁定名单版本失败");
        }
        return rosterVersionMapper.selectById(version.getId());
    }

    @Override
    public BasketDivisionRosterVersion getCurrentLockedVersion(Long divisionId) {
        if (divisionId == null) {
            return null;
        }
        return rosterVersionMapper.selectCurrentLockedByDivisionId(divisionId);
    }

    @Override
    public BasketDivisionRosterVersion getById(Long rosterVersionId) {
        if (rosterVersionId == null) {
            return null;
        }
        return rosterVersionMapper.selectById(rosterVersionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidate(Long rosterVersionId) {
        BasketDivisionRosterVersion version = requireVersion(rosterVersionId);
        if (RosterVersionStatusEnum.INVALID.getCode().equals(version.getStatus())) {
            return;
        }
        rosterVersionMapper.clearActiveUniqueKey(version.getId());
        version.setStatus(RosterVersionStatusEnum.INVALID.getCode());
        version.setIsCurrent(0);
        version.setActiveUniqueKey(null);
        version.setUpdateTime(new Date());
        if (rosterVersionMapper.updateById(version) != 1) {
            throw new ServiceException("失效名单版本失败");
        }
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketDivision lockDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectByIdForUpdate(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketDivisionRosterVersion requireVersion(Long rosterVersionId) {
        BasketDivisionRosterVersion version = rosterVersionMapper.selectById(rosterVersionId);
        if (version == null) {
            throw new ServiceException("名单版本不存在");
        }
        return version;
    }
}
