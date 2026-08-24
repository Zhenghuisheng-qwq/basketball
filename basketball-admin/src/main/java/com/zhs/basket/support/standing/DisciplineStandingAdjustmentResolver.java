package com.zhs.basket.support.standing;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.support.restriction.DisciplineClock;
import com.zhs.basket.support.restriction.EffectiveDisciplineFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从当前有效 POINT_DEDUCTION 汇总 EventTeam 积分调整。adjustment 为负数。
 */
@Component
public class DisciplineStandingAdjustmentResolver {

    @Autowired
    private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired
    private DisciplineClock disciplineClock;

    public Map<Long, Integer> resolveByEventTeamIds(Collection<Long> eventTeamIds) {
        if (eventTeamIds == null || eventTeamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = eventTeamIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketDisciplineRecord> rows = disciplineRecordMapper.selectActivePointDeductionsByEventTeamIds(ids);
        Date now = disciplineClock.now();
        List<BasketDisciplineRecord> effective = EffectiveDisciplineFilter.filter(rows, now);
        Map<Long, Integer> result = new HashMap<>();
        for (BasketDisciplineRecord record : effective) {
            if (record.getEventTeamId() == null) {
                continue;
            }
            int deduction = deductionAmount(record.getPointDeduction());
            if (deduction <= 0) {
                continue;
            }
            result.merge(record.getEventTeamId(), -deduction, Integer::sum);
        }
        return result;
    }

    static int deductionAmount(Integer stored) {
        if (stored == null || stored == 0) {
            return 0;
        }
        return stored < 0 ? -stored : stored;
    }
}
