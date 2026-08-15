package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.SeriesResolutionTypeEnum;

/**
 * 管理员正式结果兜底能力。权限仍复用 basket:v4Stage:exception，此处只封装业务状态。
 */
public final class AdminOfficialResultCapability {

    private AdminOfficialResultCapability() {
    }

    public static boolean canAdminResolveResult(BasketV4Match match) {
        return canEnterOfficialResult(match) || canCorrectOfficialResult(match);
    }

    public static boolean canEnterOfficialResult(BasketV4Match match) {
        if (match == null || !hasBothTeams(match)) {
            return false;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
            return false;
        }
        String status = match.getMatchStatus();
        return MatchStatusEnum.SCHEDULED.getCode().equals(status)
                || MatchStatusEnum.PLAYING.getCode().equals(status);
    }

    public static boolean canCorrectOfficialResult(BasketV4Match match) {
        if (match == null || !hasBothTeams(match)) {
            return false;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
            return false;
        }
        return MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus());
    }

    public static boolean canForfeitMatch(BasketV4Match match) {
        return canEnterOfficialResult(match);
    }

    public static boolean canRevokeMatchForfeit(BasketV4Match match) {
        if (match == null || !hasBothTeams(match)) {
            return false;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
            return false;
        }
        return MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus());
    }

    public static boolean canResolveSeriesManually(BasketMatchSeries series) {
        if (series == null || !hasBothSeriesTeams(series)) {
            return false;
        }
        if (MatchSeriesStatusEnum.CANCELLED.getCode().equals(series.getSeriesStatus())) {
            return false;
        }
        if (SeriesResolutionTypeEnum.FORFEIT.getCode().equals(series.getResolutionType())
                || SeriesResolutionTypeEnum.MANUAL.getCode().equals(series.getResolutionType())) {
            return false;
        }
        return true;
    }

    private static boolean hasBothTeams(BasketV4Match match) {
        return match.getHomeEventTeamId() != null && match.getHomeEventTeamId() > 0
                && match.getAwayEventTeamId() != null && match.getAwayEventTeamId() > 0;
    }

    private static boolean hasBothSeriesTeams(BasketMatchSeries series) {
        return series.getHomeEventTeamId() != null && series.getHomeEventTeamId() > 0
                && series.getAwayEventTeamId() != null && series.getAwayEventTeamId() > 0;
    }
}
