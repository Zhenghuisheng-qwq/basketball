package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class EffectiveDisciplineFilter {
    private EffectiveDisciplineFilter() {
    }

    public static boolean isCurrentlyEffective(BasketDisciplineRecord record, Date now) {
        if (record == null || now == null) {
            return false;
        }
        if (!DisciplineRecordStatusEnum.ACTIVE.name().equals(record.getRecordStatus())) {
            return false;
        }
        Date start = record.getEffectiveStartTime();
        Date end = record.getEffectiveEndTime();
        if (start != null && start.after(now)) {
            return false;
        }
        if (end != null && end.before(now)) {
            return false;
        }
        return true;
    }

    public static List<BasketDisciplineRecord> filter(List<BasketDisciplineRecord> records, Date now) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<BasketDisciplineRecord> effective = new ArrayList<BasketDisciplineRecord>();
        for (BasketDisciplineRecord record : records) {
            if (isCurrentlyEffective(record, now)) {
                effective.add(record);
            }
        }
        return effective;
    }
}
