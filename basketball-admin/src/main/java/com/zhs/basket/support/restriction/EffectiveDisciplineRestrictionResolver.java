package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class EffectiveDisciplineRestrictionResolver {

    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private DisciplineClock disciplineClock;

    public EffectiveDisciplineRestriction resolve(String subjectType, Long subjectId, Long seasonId) {
        return resolveAt(subjectType, subjectId, seasonId, disciplineClock.now());
    }

    public EffectiveDisciplineRestriction resolve(DisciplineRestrictionSubject subject, Long seasonId) {
        if (subject == null) {
            return EffectiveDisciplineRestriction.unrestricted();
        }
        return resolve(subject.getSubjectType(), subject.getSubjectId(), seasonId);
    }

    public EffectiveDisciplineRestriction resolveAt(String subjectType, Long subjectId, Long seasonId, Date now) {
        if (subjectType == null || subjectId == null) {
            return EffectiveDisciplineRestriction.unrestricted();
        }
        List<BasketDisciplineRecord> active = disciplineRecordMapper.selectActiveBySubject(subjectType, subjectId, seasonId);
        if (active == null) {
            active = Collections.emptyList();
        }
        return DisciplineRestrictionAggregator.aggregate(active, now);
    }
}
