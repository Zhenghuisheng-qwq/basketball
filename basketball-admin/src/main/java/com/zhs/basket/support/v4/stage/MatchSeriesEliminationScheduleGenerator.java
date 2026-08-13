package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 单败淘汰 BO3/BO5 系列赛赛程生成器（仅生成当前 Stage 一轮 Series + 子 Match）。
 */
@Component
public class MatchSeriesEliminationScheduleGenerator {

    private static final String RULE_CODE = "SINGLE_ELIMINATION_SERIES_V1";
    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private ByeQualificationService byeQualificationService;

    /** 仅测试注入固定种子；生产默认 null（使用无种子 shuffle）。 */
    Random random;

    ScheduleGenerationResult generate(StageExecutionContext context) {
        StageScheduleSupport.requireContext(context);
        BasketDivisionStage stage = context.getStage();
        int bestOf = stage.getBestOf();

        List<BasketMatchSeries> existingSeries = seriesMapper.selectByStageId(stage.getId());
        if (!existingSeries.isEmpty()) {
            throw new ServiceException("该阶段已存在系列赛，无法重复生成赛程");
        }

        boolean playoff = PlayoffBracketSupport.isPlayoffBracket(stage.getPairingRule());
        List<BasketDivisionStageTeam> orderedTeams = playoff
                ? PlayoffBracketSupport.orderTeams(context.getStageTeams())
                : EliminationByeSupport.orderTeamsForPairing(context.getStageTeams(), stage.getPairingRule(), random);
        int teamCount = orderedTeams.size();
        Long byeTeamId = (!playoff && EliminationByeSupport.allowsBye(stage, teamCount))
                ? EliminationByeSupport.resolveByeEventTeamId(orderedTeams) : null;
        List<BasketDivisionStageTeam> pairingTeams = byeTeamId != null
                ? EliminationByeSupport.pairingTeams(orderedTeams, byeTeamId) : orderedTeams;
        int seriesCount = EliminationByeSupport.pairingSlotCount(teamCount);
        int expectedMatchCount = seriesCount * bestOf;
        int roundNo = 1;
        int pairingSize = pairingTeams.size();

        BasketScheduleBatch batch = StageScheduleSupport.createBatch(
                scheduleBatchMapper, context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE,
                teamCount,
                expectedMatchCount,
                null);

        Date seriesMatchTime = resolveSeriesMatchTime(context.getDivision());
        Set<String> seenPairKeys = new HashSet<>();
        Date now = new Date();
        BasketDivisionStageTeam[] pair = new BasketDivisionStageTeam[2];

        for (int slot = 0; slot < seriesCount; slot++) {
            BasketDivisionStageTeam home;
            BasketDivisionStageTeam away;
            if (playoff) {
                PlayoffBracketSupport.resolveAdjacentPair(pairingTeams, slot, pair);
                home = pair[0];
                away = pair[1];
            } else {
                home = pairingTeams.get(slot);
                away = pairingTeams.get(pairingSize - 1 - slot);
            }

            BasketMatchSeries series = buildSeries(context, batch, bestOf, roundNo, slot, home, away, now);
            if (seriesMapper.insert(series) != 1) {
                throw new ServiceException("创建系列赛失败: " + series.getSeriesCode());
            }

            for (int gameNo = 1; gameNo <= bestOf; gameNo++) {
                BasketV4Match match = StageScheduleSupport.newMatch(context, batch, roundNo, slot,
                        buildSeriesGameMatchNo(context.getDivision().getId(), stage.getId(), roundNo, slot, gameNo));
                match.setSeriesId(series.getId());
                match.setSeriesGameNo(gameNo);
                match.setHomeEventTeamId(home.getEventTeamId());
                match.setAwayEventTeamId(away.getEventTeamId());
                if (seriesMatchTime != null) {
                    match.setMatchTime(seriesMatchTime);
                }
                if (gameNo == 1) {
                    StageScheduleSupport.validateMatchPair(match, context, roundNo, slot + 1, seenPairKeys);
                } else {
                    StageScheduleSupport.validateMatchPair(match, context, roundNo, slot + 1, null);
                }
                StageScheduleSupport.insertMatch(matchMapper, match);
            }
        }

        if (byeTeamId != null) {
            byeQualificationService.upsertByePreview(stage, orderedTeams);
        }

        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, roundNo);
    }

    private BasketMatchSeries buildSeries(StageExecutionContext context,
                                          BasketScheduleBatch batch,
                                          int bestOf,
                                          int roundNo,
                                          int slot,
                                          BasketDivisionStageTeam home,
                                          BasketDivisionStageTeam away,
                                          Date now) {
        BasketDivision division = context.getDivision();
        BasketDivisionStage stage = context.getStage();

        BasketMatchSeries series = new BasketMatchSeries();
        series.setDivisionId(division.getId());
        series.setStageId(stage.getId());
        series.setScheduleBatchId(batch.getId());
        series.setSeriesCode(buildSeriesCode(division.getId(), stage.getId(), roundNo, slot));
        series.setRoundNo(roundNo);
        series.setBracketSlot(slot);
        series.setHomeEventTeamId(home.getEventTeamId());
        series.setAwayEventTeamId(away.getEventTeamId());
        series.setBestOf(bestOf);
        series.setHomeWinCount(0);
        series.setAwayWinCount(0);
        series.setWinnerEventTeamId(null);
        series.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        series.setSourceSeriesIdHome(null);
        series.setSourceSeriesIdAway(null);
        series.setCreateTime(now);
        series.setUpdateTime(now);
        series.setIsDelete(NOT_DELETED);
        return series;
    }

    public static String buildSeriesCode(Long divisionId, Long stageId, int roundNo, int bracketSlot) {
        return String.format("V4-%d-SER-%d-R%d-S%02d", divisionId, stageId, roundNo, bracketSlot + 1);
    }

    public static String buildSeriesGameMatchNo(Long divisionId, Long stageId, int roundNo, int bracketSlot, int gameNo) {
        return String.format("V4-%d-SER-%d-R%d-S%02d-G%d", divisionId, stageId, roundNo, bracketSlot + 1, gameNo);
    }

    private Date resolveSeriesMatchTime(BasketDivision division) {
        if (division == null) {
            return null;
        }
        return division.getMatchStartTime();
    }
}
