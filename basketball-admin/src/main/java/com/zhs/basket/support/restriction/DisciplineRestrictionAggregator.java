package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketDisciplineRestriction;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.mapper.BasketDisciplineRestrictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class DisciplineRestrictionAggregator {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketDisciplineRestrictionMapper restrictionMapper;
    @Autowired private DisciplineClock disciplineClock;

    public static EffectiveDisciplineRestriction aggregate(List<BasketDisciplineRecord> records, Date now) {
        EffectiveDisciplineRestriction acc = EffectiveDisciplineRestriction.unrestricted();
        List<BasketDisciplineRecord> effective = EffectiveDisciplineFilter.filter(records, now);
        for (BasketDisciplineRecord record : effective) {
            acc = acc.merge(DisciplineRestrictionContributionPolicy.contribute(record));
        }
        return acc;
    }

    public EffectiveDisciplineRestriction recompute(BasketDisciplineRecord record) {
        DisciplineRestrictionSubject subject = DisciplineRestrictionSubject.from(record);
        if (subject == null) {
            return EffectiveDisciplineRestriction.unrestricted();
        }
        return recompute(subject, record.getSeasonId());
    }

    public EffectiveDisciplineRestriction recompute(DisciplineRestrictionSubject subject, Long seasonId) {
        if (subject == null || subject.getSubjectId() == null) {
            return EffectiveDisciplineRestriction.unrestricted();
        }
        Date now = disciplineClock.now();
        BasketDisciplineRestriction existing = lockOrCreateProjection(subject, seasonId, now);
        List<BasketDisciplineRecord> active = disciplineRecordMapper.selectActiveBySubjectForUpdate(
                subject.getSubjectType(), subject.getSubjectId(), seasonId);
        if (active == null) {
            active = Collections.emptyList();
        }
        EffectiveDisciplineRestriction effective = aggregate(active, now);
        persist(existing, subject, seasonId, effective, now);
        return effective;
    }

    private BasketDisciplineRestriction lockOrCreateProjection(DisciplineRestrictionSubject subject, Long seasonId,
            Date now) {
        BasketDisciplineRestriction existing = restrictionMapper.selectBySubjectForUpdate(
                subject.getSubjectType(), subject.getSubjectId(), seasonId);
        if (existing != null) {
            return existing;
        }
        BasketDisciplineRestriction placeholder = newPlaceholder(subject, seasonId, now);
        try {
            restrictionMapper.insert(placeholder);
            return placeholder;
        } catch (DuplicateKeyException ignored) {
            existing = restrictionMapper.selectBySubjectForUpdate(
                    subject.getSubjectType(), subject.getSubjectId(), seasonId);
            if (existing != null) {
                return existing;
            }
            throw ignored;
        }
    }

    private void persist(BasketDisciplineRestriction existing, DisciplineRestrictionSubject subject, Long seasonId,
            EffectiveDisciplineRestriction effective, Date now) {
        int activeFlag = effective.isUnrestricted() ? 0 : 1;
        if (existing == null || existing.getId() == null) {
            BasketDisciplineRestriction row = toRow(subject, seasonId, effective, activeFlag, now);
            try {
                restrictionMapper.insert(row);
            } catch (DuplicateKeyException ignored) {
                BasketDisciplineRestriction locked = restrictionMapper.selectBySubjectForUpdate(
                        subject.getSubjectType(), subject.getSubjectId(), seasonId);
                if (locked != null) {
                    updateProjection(locked.getId(), effective, activeFlag);
                }
            }
            return;
        }
        try {
            updateProjection(existing.getId(), effective, activeFlag);
        } catch (DuplicateKeyException ignored) {
            updateProjection(existing.getId(), effective, 1);
        }
    }

    private void updateProjection(Long id, EffectiveDisciplineRestriction effective, int activeFlag) {
        restrictionMapper.updateProjection(id, effective.flag(effective.isAllowRegistration()),
                effective.flag(effective.isAllowTransfer()), effective.flag(effective.isAllowCaptain()),
                effective.flag(effective.isAllowLineup()), effective.flag(effective.isAllowParticipation()),
                effective.getRemainingMatchSuspensions(), effective.getEffectiveDisciplineId(), activeFlag);
    }

    private BasketDisciplineRestriction newPlaceholder(DisciplineRestrictionSubject subject, Long seasonId, Date now) {
        return toRow(subject, seasonId, EffectiveDisciplineRestriction.unrestricted(), 0, now);
    }

    private BasketDisciplineRestriction toRow(DisciplineRestrictionSubject subject, Long seasonId,
            EffectiveDisciplineRestriction effective, int activeFlag, Date now) {
        BasketDisciplineRestriction row = new BasketDisciplineRestriction();
        row.setSubjectType(subject.getSubjectType());
        row.setSubjectId(subject.getSubjectId());
        row.setSeasonId(seasonId);
        row.setAllowRegistration(effective.flag(effective.isAllowRegistration()));
        row.setAllowTransfer(effective.flag(effective.isAllowTransfer()));
        row.setAllowCaptain(effective.flag(effective.isAllowCaptain()));
        row.setAllowLineup(effective.flag(effective.isAllowLineup()));
        row.setAllowParticipation(effective.flag(effective.isAllowParticipation()));
        row.setRemainingMatchSuspensions(effective.getRemainingMatchSuspensions());
        row.setEffectiveDisciplineId(effective.getEffectiveDisciplineId());
        row.setActiveFlag(activeFlag);
        row.setEffectiveStartTime(null);
        row.setEffectiveEndTime(null);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        return row;
    }
}
