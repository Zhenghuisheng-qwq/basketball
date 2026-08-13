package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.ScheduleStatusEnum;
import com.zhs.basket.enums.v4.ScheduleUnitTypeEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ScheduleUnitResolver {

    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;

    public List<ScheduleUnitView> listUnits(BasketScheduleBatch batch) {
        if (batch == null || batch.getId() == null) {
            return List.of();
        }
        Map<Long, String> teamNames = loadTeamNames(batch.getDivisionId());
        List<ScheduleUnitView> units = new ArrayList<>();
        List<BasketMatchSeries> seriesList = matchSeriesMapper.selectByScheduleBatchId(batch.getId());
        for (BasketMatchSeries series : seriesList) {
            units.add(fromSeries(series, batch, teamNames));
        }
        List<BasketV4Match> matches = matchMapper.selectByStageId(batch.getStageId());
        for (BasketV4Match match : matches) {
            if (!Objects.equals(batch.getId(), match.getScheduleBatchId())) {
                continue;
            }
            if (match.getSeriesId() != null) {
                continue;
            }
            units.add(fromMatch(match, batch, teamNames));
        }
        units.sort(Comparator.comparing(ScheduleUnitView::getRoundNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScheduleUnitView::getMatchOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScheduleUnitView::getUnitId, Comparator.nullsLast(Long::compareTo)));
        return units;
    }

    public ScheduleUnitView requireUnit(BasketScheduleBatch batch, String unitType, Long unitId) {
        ScheduleUnitTypeEnum type = ScheduleUnitTypeEnum.fromCode(unitType);
        if (type == null || unitId == null) {
            throw new ServiceException("排期单位不完整");
        }
        Map<Long, String> teamNames = loadTeamNames(batch.getDivisionId());
        if (type == ScheduleUnitTypeEnum.MATCH_SERIES) {
            BasketMatchSeries series = matchSeriesMapper.selectById(unitId);
            if (series == null || !Objects.equals(series.getScheduleBatchId(), batch.getId())
                    || !Objects.equals(series.getStageId(), batch.getStageId())) {
                throw new ServiceException("系列赛不属于当前赛段结构");
            }
            return fromSeries(series, batch, teamNames);
        }
        BasketV4Match match = matchMapper.selectById(unitId);
        if (match == null || match.getSeriesId() != null
                || !Objects.equals(match.getScheduleBatchId(), batch.getId())
                || !Objects.equals(match.getStageId(), batch.getStageId())) {
            throw new ServiceException("比赛不属于当前赛段结构，或不是独立排期对象");
        }
        return fromMatch(match, batch, teamNames);
    }

    public void applySchedule(BasketScheduleBatch batch, ScheduleUnitView unit, LocalDate date, LocalTime time, String source) {
        ScheduleUnitTypeEnum type = ScheduleUnitTypeEnum.fromCode(unit.getUnitType());
        String status = (date == null || time == null)
                ? ScheduleStatusEnum.UNSCHEDULED.getCode()
                : ScheduleStatusEnum.SCHEDULED.getCode();
        if (type == ScheduleUnitTypeEnum.MATCH_SERIES) {
            BasketMatchSeries series = matchSeriesMapper.selectById(unit.getUnitId());
            series.setScheduledDate(date);
            series.setScheduledStartTime(time);
            series.setScheduleSource(source);
            series.setScheduleStatus(status);
            if (matchSeriesMapper.updateSchedule(series) != 1) {
                throw new ServiceException("更新系列赛排期失败");
            }
            return;
        }
        BasketV4Match match = matchMapper.selectById(unit.getUnitId());
        match.setMatchTime(ScheduleTimes.toDate(date, time));
        match.setScheduleSource(source);
        match.setScheduleStatus(status);
        if (matchMapper.updateSchedule(match) != 1) {
            throw new ServiceException("更新比赛排期失败");
        }
    }

    private ScheduleUnitView fromSeries(BasketMatchSeries series, BasketScheduleBatch batch, Map<Long, String> teamNames) {
        ScheduleUnitView view = base(batch);
        view.setUnitType(ScheduleUnitTypeEnum.MATCH_SERIES.getCode());
        view.setUnitId(series.getId());
        view.setRoundNo(series.getRoundNo());
        view.setMatchOrder(series.getBracketSlot());
        view.setBestOf(series.getBestOf());
        view.setHomeParticipantId(positive(series.getHomeEventTeamId()));
        view.setAwayParticipantId(positive(series.getAwayEventTeamId()));
        view.setHomeSourceType(inferSourceType(series.getHomeSourceType(), series.getSourceSeriesIdHome()));
        view.setAwaySourceType(inferSourceType(series.getAwaySourceType(), series.getSourceSeriesIdAway()));
        view.setHomeSourceDisplay(inferDisplay(series.getHomeSourceDisplay(), view.getHomeSourceType()));
        view.setAwaySourceDisplay(inferDisplay(series.getAwaySourceDisplay(), view.getAwaySourceType()));
        view.setHomeParticipantName(resolveName(series.getHomeEventTeamId(), view.getHomeSourceDisplay(), teamNames));
        view.setAwayParticipantName(resolveName(series.getAwayEventTeamId(), view.getAwaySourceDisplay(), teamNames));
        view.setScheduledDate(ScheduleTimes.formatDate(series.getScheduledDate()));
        view.setScheduledStartTime(ScheduleTimes.formatTime(series.getScheduledStartTime()));
        view.setScheduleSource(series.getScheduleSource());
        view.setScheduleStatus(series.getScheduleStatus() == null
                ? ScheduleStatusEnum.UNSCHEDULED.getCode() : series.getScheduleStatus());
        view.setSeriesStatus(series.getSeriesStatus());
        view.setRequired(!isBye(series));
        view.setParticipantResolutionStatus(resolutionStatus(series.getHomeEventTeamId(), series.getAwayEventTeamId(),
                view.getHomeSourceType(), view.getAwaySourceType(), isBye(series)));
        fillEditable(view, batch);
        return view;
    }

    private ScheduleUnitView fromMatch(BasketV4Match match, BasketScheduleBatch batch, Map<Long, String> teamNames) {
        ScheduleUnitView view = base(batch);
        view.setUnitType(ScheduleUnitTypeEnum.MATCH.getCode());
        view.setUnitId(match.getId());
        view.setRoundNo(match.getRoundNo());
        view.setMatchOrder(match.getBracketSlot() != null ? match.getBracketSlot() : 0);
        view.setBestOf(1);
        view.setHomeParticipantId(positive(match.getHomeEventTeamId()));
        view.setAwayParticipantId(positive(match.getAwayEventTeamId()));
        view.setHomeParticipantName(resolveName(match.getHomeEventTeamId(), null, teamNames));
        view.setAwayParticipantName(resolveName(match.getAwayEventTeamId(), null, teamNames));
        view.setScheduledDate(ScheduleTimes.formatDate(ScheduleTimes.toLocalDate(match.getMatchTime())));
        view.setScheduledStartTime(ScheduleTimes.formatTime(ScheduleTimes.toLocalTime(match.getMatchTime())));
        view.setScheduleSource(match.getScheduleSource());
        String status = match.getScheduleStatus();
        if (status == null || status.isEmpty()) {
            status = match.getMatchTime() == null
                    ? ScheduleStatusEnum.UNSCHEDULED.getCode()
                    : ScheduleStatusEnum.SCHEDULED.getCode();
        }
        view.setScheduleStatus(status);
        view.setMatchStatus(match.getMatchStatus());
        view.setRequired(true);
        view.setParticipantResolutionStatus(resolutionStatus(match.getHomeEventTeamId(), match.getAwayEventTeamId(),
                null, null, false));
        fillEditable(view, batch);
        return view;
    }

    private ScheduleUnitView base(BasketScheduleBatch batch) {
        ScheduleUnitView view = new ScheduleUnitView();
        view.setDivisionId(batch.getDivisionId());
        view.setStageId(batch.getStageId());
        view.setStructureBatchId(batch.getId());
        view.setPublishStatus(batch.getPublishStatus());
        return view;
    }

    private void fillEditable(ScheduleUnitView view, BasketScheduleBatch batch) {
        boolean published = PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus());
        boolean active = StructureStatusEnum.ACTIVE.getCode().equals(batch.getStructureStatus());
        boolean finished = MatchStatusEnum.FINISHED.getCode().equals(view.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(view.getMatchStatus())
                || "COMPLETED".equals(view.getSeriesStatus())
                || "FORFEITED".equals(view.getSeriesStatus());
        view.setCanEditSchedule(active && !published && !finished && view.isRequired());
    }

    private boolean isBye(BasketMatchSeries series) {
        return ParticipantSourceTypeEnum.BYE.getCode().equals(series.getHomeSourceType())
                || ParticipantSourceTypeEnum.BYE.getCode().equals(series.getAwaySourceType());
    }

    private String resolutionStatus(Long homeId, Long awayId, String homeSource, String awaySource, boolean bye) {
        if (bye) {
            return "BYE";
        }
        boolean homeResolved = positive(homeId) != null;
        boolean awayResolved = positive(awayId) != null;
        if (homeResolved && awayResolved) {
            return "RESOLVED";
        }
        if (isDeterministic(homeSource) || isDeterministic(awaySource) || !homeResolved || !awayResolved) {
            return "TBD";
        }
        return "UNRESOLVED";
    }

    private boolean isDeterministic(String sourceType) {
        return ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(sourceType)
                || ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode().equals(sourceType);
    }

    private Long positive(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private String resolveName(Long teamId, String sourceDisplay, Map<Long, String> teamNames) {
        Long id = positive(teamId);
        if (id != null) {
            String name = teamNames.get(id);
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        if (sourceDisplay != null && !sourceDisplay.isBlank()) {
            return sourceDisplay;
        }
        return id == null ? "待定" : ("球队#" + id);
    }

    private String inferSourceType(String sourceType, Long sourceSeriesId) {
        if (sourceType != null && !sourceType.isBlank()) {
            return sourceType;
        }
        return sourceSeriesId == null ? sourceType : ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode();
    }

    private String inferDisplay(String display, String sourceType) {
        if (display != null && !display.isBlank()) {
            return display;
        }
        if (ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode().equals(sourceType)) {
            return "待定胜者";
        }
        if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(sourceType)) {
            return "待定";
        }
        return display;
    }

    private Map<Long, String> loadTeamNames(Long divisionId) {
        Map<Long, String> map = new HashMap<>();
        if (divisionId == null) {
            return map;
        }
        List<BasketEventTeam> teams = eventTeamMapper.selectActiveByDivisionId(divisionId);
        for (BasketEventTeam team : teams) {
            map.put(team.getId(), team.getTeamNameSnapshot());
        }
        return map;
    }
}
