package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineRecordStatusEnum;

import java.util.Calendar;
import java.util.Date;

final class DisciplineRestrictionFixtures {
    private DisciplineRestrictionFixtures() {
    }

    static Date at(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    static BasketDisciplineRecord record(long id, String targetType, String penaltyCode, Long subjectId,
            Long seasonId, Integer remaining, Date start, Date end) {
        BasketDisciplineRecord row = new BasketDisciplineRecord();
        row.setId(id);
        row.setTargetType(targetType);
        row.setPenaltyCode(penaltyCode);
        row.setSeasonId(seasonId);
        row.setRecordStatus(DisciplineRecordStatusEnum.ACTIVE.name());
        row.setMatchSuspensionRemaining(remaining);
        row.setEffectiveStartTime(start);
        row.setEffectiveEndTime(end);
        if ("PLAYER".equals(targetType)) {
            row.setPlayerId(subjectId);
        } else if ("EVENT_TEAM".equals(targetType)) {
            row.setEventTeamId(subjectId);
        } else if ("PERMANENT_TEAM".equals(targetType)) {
            row.setTeamId(subjectId);
        } else if ("ACCOUNT".equals(targetType)) {
            row.setMemberId(subjectId);
        }
        return row;
    }
}
