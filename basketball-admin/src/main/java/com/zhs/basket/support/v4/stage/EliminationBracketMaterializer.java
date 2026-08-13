package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.bracket.BracketSlot;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import com.zhs.basket.support.v4.bracket.EliminationBoMapping;
import com.zhs.basket.support.v4.bracket.EliminationCapacity;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic single-elimination materializer. Consumes List&lt;BracketSlot&gt; only.
 */
@Component
public class EliminationBracketMaterializer {

    private static final String RULE_CODE = "SINGLE_ELIMINATION_V1";

    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private MatchSeriesSupport matchSeriesSupport;

    public ScheduleGenerationResult materialize(StageExecutionContext context, BracketSlotPlan plan) {
        StageScheduleSupport.requireContext(context);
        if (plan == null || plan.getSlots() == null || plan.getCapacity() == null) {
            throw new ServiceException("淘汰签位方案不完整");
        }
        EliminationCapacity capacity = plan.getCapacity();
        List<BracketSlot> slots = plan.getSlots();
        if (slots.size() != capacity.getCapacity()) {
            throw new ServiceException("签位数量必须等于 bracket capacity");
        }
        validateNoFakeByeMatch(slots);

        int roundCount = capacity.getRoundCount();
        boolean useSeries = EliminationBoMapping.anySeriesBestOf(context.getStage(), roundCount);
        int expectedMatchCount = countExpectedMatches(context, capacity, useSeries);

        BasketScheduleBatch batch = StageScheduleSupport.createBatch(
                scheduleBatchMapper, context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE,
                capacity.getParticipantCount(),
                expectedMatchCount,
                null);
        if (context.getPairingExecutionId() != null) {
            batch.setPairingExecutionId(context.getPairingExecutionId());
            scheduleBatchMapper.updateById(batch);
        }

        List<RoundNode> previous = materializeFirstRound(context, batch, slots, capacity, roundCount, useSeries);
        for (int roundNo = 2; roundNo <= roundCount; roundNo++) {
            previous = materializeLaterRound(context, batch, previous, roundNo, roundCount, useSeries);
        }
        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, roundCount);
    }

    private List<RoundNode> materializeFirstRound(StageExecutionContext context,
                                                  BasketScheduleBatch batch,
                                                  List<BracketSlot> slots,
                                                  EliminationCapacity capacity,
                                                  int roundCount,
                                                  boolean useSeries) {
        int pairCount = slots.size() / 2;
        List<RoundNode> nodes = new ArrayList<>(pairCount);
        int actualCreated = 0;
        for (int slot = 0; slot < pairCount; slot++) {
            BracketSlot home = slots.get(slot * 2);
            BracketSlot away = slots.get(slot * 2 + 1);
            RoundNode node = new RoundNode();
            node.roundNo = 1;
            node.slot = slot;
            if (home.isBye() && away.isBye()) {
                throw new ServiceException("签位不能出现 BYE vs BYE");
            }
            if (home.isBye()) {
                node.byeAdvance = true;
                node.winnerTeamId = away.effectiveEventTeamId();
            } else if (away.isBye()) {
                node.byeAdvance = true;
                node.winnerTeamId = home.effectiveEventTeamId();
            } else {
                int bestOf = EliminationBoMapping.bestOf(context.getStage(), 1, roundCount);
                boolean placeholderPending = home.isUnresolvedPlaceholder() || away.isUnresolvedPlaceholder();
                createPairing(context, batch, node, bestOf, useSeries,
                        home.effectiveEventTeamId(), away.effectiveEventTeamId(), null, null,
                        home, away, placeholderPending);
                actualCreated++;
            }
            nodes.add(node);
        }
        if (actualCreated != capacity.getFirstRoundActual()) {
            throw new ServiceException("第一轮实际对阵数量与公式不符");
        }
        return nodes;
    }

    private List<RoundNode> materializeLaterRound(StageExecutionContext context,
                                                  BasketScheduleBatch batch,
                                                  List<RoundNode> previous,
                                                  int roundNo,
                                                  int roundCount,
                                                  boolean useSeries) {
        int pairCount = previous.size() / 2;
        List<RoundNode> nodes = new ArrayList<>(pairCount);
        int bestOf = EliminationBoMapping.bestOf(context.getStage(), roundNo, roundCount);
        for (int slot = 0; slot < pairCount; slot++) {
            RoundNode homeSrc = previous.get(slot * 2);
            RoundNode awaySrc = previous.get(slot * 2 + 1);
            RoundNode node = new RoundNode();
            node.roundNo = roundNo;
            node.slot = slot;
            Long homeId = homeSrc.byeAdvance ? homeSrc.winnerTeamId : null;
            Long awayId = awaySrc.byeAdvance ? awaySrc.winnerTeamId : null;
            Long sourceHome = homeSrc.byeAdvance ? null : seriesOrMatchSource(homeSrc, useSeries);
            Long sourceAway = awaySrc.byeAdvance ? null : seriesOrMatchSource(awaySrc, useSeries);
            if (homeSrc.byeAdvance && awaySrc.byeAdvance) {
                createPairing(context, batch, node, bestOf, useSeries, homeId, awayId, null, null, null, null, false);
            } else {
                createPairing(context, batch, node, bestOf, useSeries, homeId, awayId, sourceHome, sourceAway, null, null, false);
            }
            nodes.add(node);
        }
        return nodes;
    }

    private void createPairing(StageExecutionContext context,
                               BasketScheduleBatch batch,
                               RoundNode node,
                               int bestOf,
                               boolean useSeries,
                               Long homeId,
                               Long awayId,
                               Long sourceHome,
                               Long sourceAway,
                               BracketSlot homeSlot,
                               BracketSlot awaySlot,
                               boolean placeholderPending) {
        if (useSeries || bestOf > 1 || placeholderPending) {
            int seriesBestOf = Math.max(bestOf, 1);
            if (placeholderPending) {
                BasketMatchSeries series = matchSeriesSupport.createSeriesSkeleton(
                        context, batch, seriesBestOf, node.roundNo, node.slot,
                        homeId, awayId, sourceHome, sourceAway,
                        homeSlot == null ? null : homeSlot.getSourceType(),
                        awaySlot == null ? null : awaySlot.getSourceType(),
                        homeSlot == null ? null : homeSlot.getDisplayName(),
                        awaySlot == null ? null : awaySlot.getDisplayName());
                node.series = series;
            } else {
                BasketMatchSeries series = matchSeriesSupport.createSeriesWithGames(
                        context, batch, seriesBestOf, node.roundNo, node.slot,
                        homeId, awayId, sourceHome, sourceAway, homeId != null && awayId != null);
                if (homeSlot != null) {
                    series.setHomeSourceType(homeSlot.getSourceType());
                    series.setHomeSourceDisplay(homeSlot.getDisplayName());
                }
                if (awaySlot != null) {
                    series.setAwaySourceType(awaySlot.getSourceType());
                    series.setAwaySourceDisplay(awaySlot.getDisplayName());
                }
                if (homeSlot != null || awaySlot != null) {
                    matchSeriesSupport.updateSourceLabels(series);
                }
                node.series = series;
            }
        } else {
            BasketV4Match match = StageScheduleSupport.newMatch(context, batch, node.roundNo, node.slot,
                    String.format("V4-%d-E%d-R%d-S%02d",
                            context.getDivision().getId(), context.getStage().getId(), node.roundNo, node.slot + 1));
            match.setHomeEventTeamId(homeId);
            match.setAwayEventTeamId(awayId);
            if (!useSeries) {
                match.setSourceMatchIdHome(sourceHome);
                match.setSourceMatchIdAway(sourceAway);
            }
            if (homeId != null && awayId != null) {
                StageScheduleSupport.validateMatchPair(match, context, node.roundNo, node.slot + 1, null);
            }
            StageScheduleSupport.insertMatch(matchMapper, match);
            node.match = match;
        }
    }

    private Long seriesOrMatchSource(RoundNode node, boolean useSeries) {
        if (useSeries) {
            return node.series != null ? node.series.getId() : null;
        }
        return node.match != null ? node.match.getId() : null;
    }

    private int countExpectedMatches(StageExecutionContext context, EliminationCapacity capacity, boolean useSeries) {
        int roundCount = capacity.getRoundCount();
        if (!useSeries) {
            return capacity.getTotalActualSeries();
        }
        int total = capacity.getFirstRoundActual()
                * EliminationBoMapping.bestOf(context.getStage(), 1, roundCount);
        for (int roundNo = 2; roundNo <= roundCount; roundNo++) {
            int pairsThisRound = capacity.getCapacity() / (1 << roundNo);
            total += pairsThisRound * EliminationBoMapping.bestOf(context.getStage(), roundNo, roundCount);
        }
        return total;
    }

    private void validateNoFakeByeMatch(List<BracketSlot> slots) {
        for (BracketSlot slot : slots) {
            if (slot.isBye() && (slot.getEventTeamId() != null || slot.getResolvedEventTeamId() != null)) {
                throw new ServiceException("BYE 签位不能绑定球队");
            }
            if (!slot.isBye() && slot.effectiveEventTeamId() == null && !slot.isUnresolvedPlaceholder()) {
                throw new ServiceException("非 BYE 签位必须绑定正式参赛队或 GROUP_RANK 占位来源");
            }
        }
    }

    private static final class RoundNode {
        int roundNo;
        int slot;
        boolean byeAdvance;
        Long winnerTeamId;
        BasketMatchSeries series;
        BasketV4Match match;
    }
}
