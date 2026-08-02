package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.vo.MatchDetailVo;
import com.zhs.basket.vo.MatchListVo;

import java.math.BigDecimal;
import java.util.Date;

public interface IBasketMatchValidationService {
    BasketMatch requireMatch(Long matchId);
    MatchDetailVo requireDetail(Long matchId);
    void validateTeamsForSeason(Long seasonId, Long homeTeamId, Long awayTeamId);
    void validateTeamsInGroup(Long groupId, Long homeTeamId, Long awayTeamId);
    void validateHierarchy(Long seasonId, Long stageId, Long roundId, Long groupId);
    void checkScheduleConflict(Long excludeMatchId, Long venueId, Long homeTeamId, Long awayTeamId,
                               Date startTime, Date endTime);
    Date resolveEndTime(Date startTime, Date endTime);
    void writeStatusLog(BasketMatch match, Integer beforeStatus, MatchChangeTypeEnum changeType, String reason);
    void writeSystemStatusLog(BasketMatch match, Integer beforeStatus, MatchChangeTypeEnum changeType, String reason);
    MatchListVo enrichListVo(MatchListVo vo);
    MatchDetailVo enrichDetailVo(MatchDetailVo vo);
    void requirePublishedForOps(BasketMatch match);
    BigDecimal computePct(Integer made, Integer attempted);
    int computePointsFromShooting(Integer fgm, Integer tpm, Integer ftm);
    void validateShooting(Integer made, Integer attempted, String label);
    void validateThreeWithinFg(Integer fgm, Integer tpm, String label);
    void validateTheoreticalPoints(Integer points, Integer fgm, Integer tpm, Integer ftm, String label);
}
