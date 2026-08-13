package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Pairing;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.Round;
import com.zhs.basket.support.v4.BasketV4RoundRobinScheduler.TimeSlotAllocator;
import com.zhs.basket.support.v4.BestOfValues;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Round robin stage strategy. cycles=1/2 share one circle-method engine.
 */
@Component
public class RoundRobinStageScheduleStrategy extends AbstractStageScheduleStrategy {

    @Autowired
    private MatchSeriesSupport matchSeriesSupport;

    @Override
    public boolean supports(BasketDivisionStage stage) {
        if (!CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
            return false;
        }
        return !isGroupStage(stage);
    }

    @Override
    public void validate(StageExecutionContext context) {
        requireContext(context);
        BestOfValues.requireLegal(context.getStage().getBestOf());
        validateTeamsPresent(context);
        int cycles = resolveCycles(context.getStage());
        if (cycles != 1 && cycles != 2) {
            throw new ServiceException("round_robin_cycles 仅支持 1 或 2");
        }
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        return buildRoundRobinPreview(context.getStageTeams().size(), resolveCycles(context.getStage()));
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        int cycles = resolveCycles(context.getStage());
        int bestOf = context.getStage().getBestOf() == null ? 1 : context.getStage().getBestOf();
        List<Long> teamIds = BasketV4RoundRobinScheduler.sortedTeamIds(resolveTeamIds(context.getStageTeams()));
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(teamIds, cycles);
        int pairingCount = BasketV4RoundRobinScheduler.countMatches(rounds);
        int expectedMatchCount = pairingCount * bestOf;

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.ROUND_ROBIN,
                BasketV4RoundRobinScheduler.ruleCode(),
                teamIds.size(),
                expectedMatchCount,
                null);

        Date startFrom = context.getDivision().getMatchStartTime() != null
                ? context.getDivision().getMatchStartTime() : new Date();
        TimeSlotAllocator allocator = new TimeSlotAllocator(startFrom);
        Set<String> seenOrderedKeys = new HashSet<>();

        for (Round round : rounds) {
            int seq = 1;
            for (Pairing pairing : round.getPairings()) {
                String orderedKey = pairing.getHomeEventTeamId() + ">" + pairing.getAwayEventTeamId();
                if (!seenOrderedKeys.add(orderedKey)) {
                    throw new ServiceException("赛程对阵无效：出现重复主客顺序对阵");
                }
                if (bestOf <= 1) {
                    BasketV4Match match = newMatch(context, batch, round.getRoundNo(), null,
                            BasketV4RoundRobinScheduler.buildMatchNo(context.getDivision().getId(), round.getRoundNo(), seq));
                    match.setMatchTime(allocator.allocate());
                    match.setHomeEventTeamId(pairing.getHomeEventTeamId());
                    match.setAwayEventTeamId(pairing.getAwayEventTeamId());
                    validateAndInsertMatch(match, context, round.getRoundNo(), seq, null);
                } else {
                    matchSeriesSupport.createSeriesWithGames(context, batch, bestOf, round.getRoundNo(), seq - 1,
                            pairing.getHomeEventTeamId(), pairing.getAwayEventTeamId(), null, null, true);
                }
                seq++;
            }
        }

        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, rounds.size());
    }

    private static int resolveCycles(BasketDivisionStage stage) {
        Integer cycles = stage.getRoundRobinCycles();
        return cycles == null || cycles <= 0 ? 1 : cycles;
    }
}
