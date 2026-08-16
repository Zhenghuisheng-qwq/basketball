package com.zhs.support.v4;

import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.v4.publicapi.V4PublicEventTeamItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchDetailResp;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesDetailResp;
import com.zhs.response.v4.publicapi.V4PublicScheduleItemResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;

public final class EventTeamPublicDisplaySupport {

    public static final String STATUS_DISQUALIFIED = "DISQUALIFIED";
    public static final String RESULT_FORFEIT = "FORFEIT";
    public static final String RESULT_EVENT_DISQUALIFICATION = "EVENT_DISQUALIFICATION";
    public static final String REASON_DQ = "赛事资格取消";
    public static final String REASON_OPPONENT_DQ = "对手取消赛事资格";
    public static final String LABEL_FORFEIT_LOSS = "判负";
    public static final String LABEL_FORFEIT_WIN = "判胜";

    private EventTeamPublicDisplaySupport() {
    }

    public static boolean isDisqualified(BasketEventTeam team) {
        return team != null && STATUS_DISQUALIFIED.equalsIgnoreCase(team.getEventTeamStatus());
    }

    public static boolean isDisqualified(String status) {
        return STATUS_DISQUALIFIED.equalsIgnoreCase(status);
    }

    public static String statusName(String status) {
        if (STATUS_DISQUALIFIED.equalsIgnoreCase(status)) {
            return "赛事资格已取消";
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "有效";
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return "无效";
        }
        return status;
    }

    public static void fillEventTeam(V4PublicEventTeamItemResp vo, BasketEventTeam team) {
        if (vo == null || team == null) {
            return;
        }
        vo.setEventTeamStatus(team.getEventTeamStatus());
        vo.setEventTeamStatusName(statusName(team.getEventTeamStatus()));
        vo.setDisqualified(isDisqualified(team));
    }

    public static void fillStanding(V4PublicStandingItemResp vo, BasketEventTeam team) {
        if (vo == null || team == null) {
            return;
        }
        vo.setEventTeamStatus(team.getEventTeamStatus());
        vo.setDisqualified(isDisqualified(team));
    }

    public static void fillMatch(V4PublicMatchItemResp vo, BasketV4Match match,
                                 BasketEventTeam home, BasketEventTeam away) {
        if (vo == null || match == null) {
            return;
        }
        boolean homeDq = isDisqualified(home);
        boolean awayDq = isDisqualified(away);
        vo.setHomeDisqualified(homeDq);
        vo.setAwayDisqualified(awayDq);
        applyForfeitDisplay(vo, match, homeDq, awayDq);
    }

    public static void fillMatchDetail(V4PublicMatchDetailResp vo, BasketV4Match match,
                                       BasketEventTeam home, BasketEventTeam away) {
        if (vo == null || match == null) {
            return;
        }
        boolean homeDq = isDisqualified(home);
        boolean awayDq = isDisqualified(away);
        vo.setHomeDisqualified(homeDq);
        vo.setAwayDisqualified(awayDq);
        if (!"FORFEITED".equalsIgnoreCase(match.getMatchStatus())) {
            return;
        }
        boolean dqForfeit = isDisqualificationForfeit(match.getRemark(), homeDq, awayDq, match.getWinnerEventTeamId(),
                match.getHomeEventTeamId(), match.getAwayEventTeamId());
        vo.setResultType(dqForfeit ? RESULT_EVENT_DISQUALIFICATION : RESULT_FORFEIT);
        vo.setResultReason(resolveMatchReason(match, homeDq, awayDq));
        vo.setHomeResultLabel(labelForSide(match.getWinnerEventTeamId(), match.getHomeEventTeamId()));
        vo.setAwayResultLabel(labelForSide(match.getWinnerEventTeamId(), match.getAwayEventTeamId()));
        if (dqForfeit) {
            vo.setHomeScore(null);
            vo.setAwayScore(null);
        }
    }

    public static void fillSeries(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series,
                                  BasketEventTeam home, BasketEventTeam away) {
        if (vo == null || series == null) {
            return;
        }
        boolean homeDq = isDisqualified(home);
        boolean awayDq = isDisqualified(away);
        vo.setHomeDisqualified(homeDq);
        vo.setAwayDisqualified(awayDq);
        if (!"FORFEIT".equalsIgnoreCase(series.getResolutionType())) {
            return;
        }
        boolean dq = (homeDq || awayDq) || (series.getResolutionReason() != null
                && series.getResolutionReason().contains(REASON_DQ));
        vo.setResultReason(dq ? REASON_DQ : "系列赛弃赛");
        if (dq) {
            vo.setResultSummary(buildDqSeriesSummary(vo, series));
        }
    }

    public static void fillSchedule(V4PublicScheduleItemResp item, BasketMatchSeries series,
                                    BasketEventTeam home, BasketEventTeam away) {
        if (item == null) {
            return;
        }
        boolean homeDq = isDisqualified(home);
        boolean awayDq = isDisqualified(away);
        item.setHomeDisqualified(homeDq);
        item.setAwayDisqualified(awayDq);
        if (series != null && "FORFEIT".equalsIgnoreCase(series.getResolutionType())) {
            boolean dq = homeDq || awayDq || (series.getResolutionReason() != null
                    && series.getResolutionReason().contains(REASON_DQ));
            item.setResultType(dq ? RESULT_EVENT_DISQUALIFICATION : RESULT_FORFEIT);
            item.setResultReason(dq ? REASON_DQ : "系列赛弃赛");
        }
    }

    private static void applyForfeitDisplay(V4PublicMatchItemResp vo, BasketV4Match match,
                                            boolean homeDq, boolean awayDq) {
        if (!"FORFEITED".equalsIgnoreCase(match.getMatchStatus())) {
            return;
        }
        boolean dqForfeit = isDisqualificationForfeit(match.getRemark(), homeDq, awayDq, match.getWinnerEventTeamId(),
                match.getHomeEventTeamId(), match.getAwayEventTeamId());
        vo.setResultType(dqForfeit ? RESULT_EVENT_DISQUALIFICATION : RESULT_FORFEIT);
        vo.setResultReason(resolveMatchReason(match, homeDq, awayDq));
        vo.setHomeResultLabel(labelForSide(match.getWinnerEventTeamId(), match.getHomeEventTeamId()));
        vo.setAwayResultLabel(labelForSide(match.getWinnerEventTeamId(), match.getAwayEventTeamId()));
        if (dqForfeit) {
            vo.setHomeScore(null);
            vo.setAwayScore(null);
        }
    }

    private static boolean isDisqualificationForfeit(String remark, boolean homeDq, boolean awayDq,
                                                     Long winnerId, Long homeId, Long awayId) {
        if (remark != null && remark.contains(REASON_DQ)) {
            return true;
        }
        if (winnerId == null) {
            return homeDq || awayDq;
        }
        if (winnerId.equals(homeId)) {
            return awayDq;
        }
        if (winnerId.equals(awayId)) {
            return homeDq;
        }
        return homeDq || awayDq;
    }

    private static String resolveMatchReason(BasketV4Match match, boolean homeDq, boolean awayDq) {
        if (match.getRemark() != null && match.getRemark().contains(REASON_DQ)) {
            return REASON_DQ;
        }
        Long winner = match.getWinnerEventTeamId();
        if (winner != null && winner.equals(match.getHomeEventTeamId()) && awayDq) {
            return REASON_OPPONENT_DQ;
        }
        if (winner != null && winner.equals(match.getAwayEventTeamId()) && homeDq) {
            return REASON_OPPONENT_DQ;
        }
        if (homeDq || awayDq) {
            return REASON_DQ;
        }
        return "弃权判负";
    }

    private static String labelForSide(Long winnerId, Long sideId) {
        if (winnerId == null || sideId == null) {
            return null;
        }
        return winnerId.equals(sideId) ? LABEL_FORFEIT_WIN : LABEL_FORFEIT_LOSS;
    }

    private static String buildDqSeriesSummary(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        String loser = series.getWinnerEventTeamId() != null && series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())
                ? vo.getAwayTeamName() : vo.getHomeTeamName();
        String winner = vo.getWinnerTeamName();
        if (loser != null && winner != null) {
            return loser + " 取消资格判负，" + winner + " 晋级";
        }
        return "取消资格判负";
    }
}
