package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ScheduleRecommendationTypeEnum;
import com.zhs.basket.enums.v4.ScheduleSourceEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3 League Regular: Tue/Thu/Sat, 21:00 then 22:00, two rounds per match day.
 * Round is the schedule grain; all units in a round share one date/time.
 */
@Component
public class RoundRobinScheduleRecommendationStrategy implements ScheduleRecommendationStrategy {

    static final Set<DayOfWeek> WEEKDAYS = EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
    static final LocalTime SLOT_A = LocalTime.of(21, 0);
    static final LocalTime SLOT_B = LocalTime.of(22, 0);

    @Override
    public ScheduleRecommendationTypeEnum type() {
        return ScheduleRecommendationTypeEnum.ROUND_ROBIN_STANDARD;
    }

    @Override
    public List<ScheduleUnitView> recommend(List<ScheduleUnitView> units, LocalDate startDate, boolean overwriteScheduled) {
        List<ScheduleUnitView> sorted = units.stream()
                .filter(ScheduleUnitView::isRequired)
                .sorted(Comparator.comparing(ScheduleUnitView::getRoundNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ScheduleUnitView::getMatchOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ScheduleUnitView::getUnitId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        Map<Integer, List<ScheduleUnitView>> byRound = new LinkedHashMap<>();
        for (ScheduleUnitView unit : sorted) {
            byRound.computeIfAbsent(unit.getRoundNo() == null ? 0 : unit.getRoundNo(), k -> new ArrayList<>()).add(unit);
        }
        LocalDate cursor = startDate;
        int slot = 0;
        List<ScheduleUnitView> proposed = new ArrayList<>();
        for (List<ScheduleUnitView> roundUnits : byRound.values()) {
            while (!WEEKDAYS.contains(cursor.getDayOfWeek())) {
                cursor = cursor.plusDays(1);
            }
            LocalTime time = slot == 0 ? SLOT_A : SLOT_B;
            String dateText = ScheduleTimes.formatDate(cursor);
            String timeText = ScheduleTimes.formatTime(time);
            for (ScheduleUnitView unit : roundUnits) {
                boolean already = ScheduleStatusEnum.SCHEDULED.getCode().equals(unit.getScheduleStatus())
                        && unit.getScheduledDate() != null && unit.getScheduledStartTime() != null;
                if (already && !overwriteScheduled) {
                    proposed.add(copy(unit));
                    continue;
                }
                ScheduleUnitView next = copy(unit);
                next.setScheduledDate(dateText);
                next.setScheduledStartTime(timeText);
                next.setScheduleStatus(ScheduleStatusEnum.SCHEDULED.getCode());
                next.setScheduleSource(ScheduleSourceEnum.RECOMMENDED.getCode());
                proposed.add(next);
            }
            slot++;
            if (slot == 2) {
                slot = 0;
                cursor = cursor.plusDays(1);
            }
        }
        return proposed;
    }

    private ScheduleUnitView copy(ScheduleUnitView src) {
        ScheduleUnitView copy = new ScheduleUnitView();
        copy.setUnitType(src.getUnitType());
        copy.setUnitId(src.getUnitId());
        copy.setDivisionId(src.getDivisionId());
        copy.setStageId(src.getStageId());
        copy.setStructureBatchId(src.getStructureBatchId());
        copy.setRoundNo(src.getRoundNo());
        copy.setRoundRole(src.getRoundRole());
        copy.setMatchOrder(src.getMatchOrder());
        copy.setHomeParticipantId(src.getHomeParticipantId());
        copy.setAwayParticipantId(src.getAwayParticipantId());
        copy.setHomeParticipantName(src.getHomeParticipantName());
        copy.setAwayParticipantName(src.getAwayParticipantName());
        copy.setHomeSourceType(src.getHomeSourceType());
        copy.setAwaySourceType(src.getAwaySourceType());
        copy.setHomeSourceDisplay(src.getHomeSourceDisplay());
        copy.setAwaySourceDisplay(src.getAwaySourceDisplay());
        copy.setBestOf(src.getBestOf());
        copy.setScheduledDate(src.getScheduledDate());
        copy.setScheduledStartTime(src.getScheduledStartTime());
        copy.setScheduleStatus(src.getScheduleStatus());
        copy.setScheduleSource(src.getScheduleSource());
        copy.setParticipantResolutionStatus(src.getParticipantResolutionStatus());
        copy.setPublishStatus(src.getPublishStatus());
        copy.setCanEditSchedule(src.isCanEditSchedule());
        copy.setRequired(src.isRequired());
        copy.setMatchStatus(src.getMatchStatus());
        copy.setSeriesStatus(src.getSeriesStatus());
        return copy;
    }
}
