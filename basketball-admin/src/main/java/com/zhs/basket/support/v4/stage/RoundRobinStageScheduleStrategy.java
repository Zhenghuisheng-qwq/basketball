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
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 单循环阶段赛程策略（非小组赛）。
 */
@Component
public class RoundRobinStageScheduleStrategy extends AbstractStageScheduleStrategy {

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
        validateBestOfOne(context.getStage());
        validateTeamsPresent(context);
        Integer cycles = context.getStage().getRoundRobinCycles();
        if (cycles != null && cycles > 1) {
            throw new ServiceException("当前版本暂不支持多循环（round_robin_cycles > 1）");
        }
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        return buildRoundRobinPreview(context.getStageTeams().size());
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        List<Long> teamIds = BasketV4RoundRobinScheduler.sortedTeamIds(resolveTeamIds(context.getStageTeams()));
        List<Round> rounds = BasketV4RoundRobinScheduler.generateRoundRobin(teamIds);
        int expectedMatchCount = BasketV4RoundRobinScheduler.countMatches(rounds);

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.ROUND_ROBIN,
                BasketV4RoundRobinScheduler.ruleCode(),
                teamIds.size(),
                expectedMatchCount,
                null);

        Date startFrom = context.getDivision().getMatchStartTime() != null
                ? context.getDivision().getMatchStartTime() : new Date();
        TimeSlotAllocator allocator = new TimeSlotAllocator(startFrom);
        Set<String> seenPairKeys = new HashSet<>();

        for (Round round : rounds) {
            int seq = 1;
            for (Pairing pairing : round.getPairings()) {
                BasketV4Match match = newMatch(context, batch, round.getRoundNo(), null,
                        BasketV4RoundRobinScheduler.buildMatchNo(context.getDivision().getId(), round.getRoundNo(), seq));
                match.setMatchTime(allocator.allocate());
                match.setHomeEventTeamId(pairing.getHomeEventTeamId());
                match.setAwayEventTeamId(pairing.getAwayEventTeamId());
                validateAndInsertMatch(match, context, round.getRoundNo(), seq, seenPairKeys);
                seq++;
            }
        }

        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, rounds.size());
    }
}
