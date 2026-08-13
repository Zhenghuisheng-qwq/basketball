package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.DrawRuleEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.support.v4.BestOfValues;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.basket.support.v4.MatchSeriesExecutionCapability;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.basket.support.v4.bracket.BracketParticipant;
import com.zhs.basket.support.v4.bracket.BracketSlotAllocationStrategy;
import com.zhs.basket.support.v4.bracket.BracketSlotAllocationStrategyFactory;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import com.zhs.basket.support.v4.bracket.EliminationBoMapping;
import com.zhs.basket.support.v4.bracket.EliminationCapacity;
import com.zhs.basket.support.v4.bracket.SlotAllocationRequest;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Single-elimination strategy. Reads pairing/seed/draw/bye rules, never Series type.
 */
@Component
public class SingleEliminationStageScheduleStrategy extends AbstractStageScheduleStrategy {

    private static final String RULE_CODE = "SINGLE_ELIMINATION_V1";

    @Autowired
    private EliminationBracketMaterializer materializer;
    @Autowired
    private BracketSlotAllocationStrategyFactory slotAllocationStrategyFactory;
    @Autowired
    private MatchSeriesEliminationScheduleGenerator seriesScheduleGenerator;
    @Autowired
    private ByeQualificationService byeQualificationService;

    /** Test-only shuffle seed for legacy CUSTOM RANDOM_DRAW. */
    Random random;

    @Override
    public boolean supports(BasketDivisionStage stage) {
        return CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat())
                && !StageFormatSpecification.isGrouped(stage);
    }

    @Override
    public void validate(StageExecutionContext context) {
        requireContext(context);
        validateTeamsPresent(context);
        BestOfValues.requireLegal(context.getStage().getBestOf());
        if (context.getStage().getSemifinalBestOf() != null) {
            BestOfValues.requireLegal(context.getStage().getSemifinalBestOf());
        }
        if (context.getStage().getFinalBestOf() != null) {
            BestOfValues.requireLegal(context.getStage().getFinalBestOf());
        }
        validatePairingRule(context.getStage());

        if (EliminationStageSupport.isSingleRoundNonFinalStage(context.getStage())) {
            return;
        }
        if (requiresConfirmedDraw(context.getStage()) && context.getBracketSlotPlan() == null) {
            throw new ServiceException("当前抽签尚未确认，无法生成结构");
        }
        if (materializer != null) {
            int n = context.getBracketSlotPlan() != null && context.getBracketSlotPlan().getCapacity() != null
                    ? context.getBracketSlotPlan().getCapacity().getParticipantCount()
                    : context.getStageTeams().size();
            EliminationCapacity.of(n);
            return;
        }
        int teamCount = context.getStageTeams().size();
        if (!StageScheduleMathSupport.isPowerOfTwo(teamCount)) {
            throw new ServiceException("单败淘汰阶段参赛队伍数必须为 2 的幂次（当前 " + teamCount + " 支），不支持轮空");
        }
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        int teamCount = context.getBracketSlotPlan() != null && context.getBracketSlotPlan().getCapacity() != null
                ? context.getBracketSlotPlan().getCapacity().getParticipantCount()
                : context.getStageTeams().size();
        StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
        preview.setTeamCount(teamCount);

        if (EliminationStageSupport.isSingleRoundNonFinalStage(context.getStage())) {
            int matchCount = EliminationByeSupport.pairingSlotCount(teamCount);
            preview.setTotalMatches(matchCount);
            preview.setSeriesCount(matchCount);
            preview.setRoundCount(1);
            preview.setHasBye(EliminationByeSupport.allowsBye(context.getStage(), teamCount));
            return preview;
        }

        EliminationCapacity capacity = EliminationCapacity.of(teamCount);
        preview.setRoundCount(capacity.getRoundCount());
        preview.setHasBye(capacity.getByeCount() > 0);
        preview.setSeriesCount(capacity.getTotalActualSeries());
        preview.setTotalMatches(capacity.getTotalActualSeries());
        preview.setBracketCapacity(capacity.getCapacity());
        preview.setByeCount(capacity.getByeCount());
        preview.setFirstRoundActual(capacity.getFirstRoundActual());
        preview.setTotalActualSeries(capacity.getTotalActualSeries());
        preview.getWarnings().add("bracketCapacity=" + capacity.getCapacity()
                + ", byeCount=" + capacity.getByeCount()
                + ", firstRoundActual=" + capacity.getFirstRoundActual()
                + ", totalSeries=" + capacity.getTotalActualSeries());
        preview.getWarnings().add("BO early=" + EliminationBoMapping.bestOf(context.getStage(), 1, capacity.getRoundCount())
                + ", SF=" + EliminationBoMapping.bestOf(context.getStage(), Math.max(1, capacity.getRoundCount() - 1), capacity.getRoundCount())
                + ", Final=" + EliminationBoMapping.bestOf(context.getStage(), capacity.getRoundCount(), capacity.getRoundCount()));
        return preview;
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        if (EliminationStageSupport.isSingleRoundNonFinalStage(context.getStage())) {
            if (isSeriesElimination(context.getStage())) {
                seriesScheduleGenerator.random = random;
                return seriesScheduleGenerator.generate(context);
            }
            return generateSingleRoundBo1(context);
        }
        if (materializer != null) {
            BracketSlotPlan plan = context.getBracketSlotPlan();
            if (plan == null) {
                List<BracketParticipant> participants = toParticipants(context.getStageTeams());
                BracketSlotAllocationStrategy strategy = slotAllocationStrategyFactory.resolve(
                        context.getStage().getPairingRule(), context.getStage().getDrawRule());
                plan = strategy.allocate(participants, SlotAllocationRequest.builder()
                        .pairingRule(context.getStage().getPairingRule())
                        .seedRule(context.getStage().getSeedRule())
                        .drawRule(context.getStage().getDrawRule())
                        .byeRule(context.getStage().getByeRule())
                        .build());
            }
            context.setBracketSlotPlan(plan);
            return materializer.materialize(context, plan);
        }
        if (isSeriesElimination(context.getStage())) {
            seriesScheduleGenerator.random = random;
            return seriesScheduleGenerator.generate(context);
        }
        return generateFullBracketBo1(context);
    }

    private ScheduleGenerationResult generateSingleRoundBo1(StageExecutionContext context) {
        List<BasketDivisionStageTeam> orderedTeams = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule())
                ? PlayoffBracketSupport.orderTeams(context.getStageTeams())
                : EliminationByeSupport.orderTeamsForPairing(
                context.getStageTeams(), context.getStage().getPairingRule(), random);
        int teamCount = orderedTeams.size();
        boolean playoff = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule());
        Long byeTeamId = (!playoff && EliminationByeSupport.allowsBye(context.getStage(), teamCount))
                ? EliminationByeSupport.resolveByeEventTeamId(orderedTeams) : null;
        List<BasketDivisionStageTeam> pairingTeams = byeTeamId != null
                ? EliminationByeSupport.pairingTeams(orderedTeams, byeTeamId) : orderedTeams;
        int seriesCount = EliminationByeSupport.pairingSlotCount(teamCount);

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE, teamCount, seriesCount, null);

        Set<String> seenPairKeys = new HashSet<>();
        int pairingSize = pairingTeams.size();
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
            BasketV4Match match = newMatch(context, batch, 1, slot,
                    buildEliminationMatchNo(context.getDivision().getId(), context.getStage().getId(), 1, slot));
            match.setHomeEventTeamId(home.getEventTeamId());
            match.setAwayEventTeamId(away.getEventTeamId());
            validateMatchPair(match, context, 1, slot + 1, seenPairKeys);
            insertMatch(match);
        }
        if (byeTeamId != null) {
            byeQualificationService.upsertByePreview(context.getStage(), orderedTeams);
        }
        return new ScheduleGenerationResult(batch.getId(), seriesCount, seriesCount, 1);
    }

    private ScheduleGenerationResult generateFullBracketBo1(StageExecutionContext context) {
        List<BasketDivisionStageTeam> orderedTeams = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule())
                || PairingRuleEnum.HIGH_VS_LOW.getCode().equals(context.getStage().getPairingRule())
                ? PlayoffBracketSupport.orderTeams(context.getStageTeams())
                : EliminationByeSupport.orderTeamsForPairing(
                context.getStageTeams(), context.getStage().getPairingRule(), random);
        int teamCount = orderedTeams.size();
        int roundCount = StageScheduleMathSupport.eliminationRoundCount(teamCount);
        int expectedMatchCount = StageScheduleMathSupport.eliminationMatchCount(teamCount);

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE, teamCount, expectedMatchCount, null);

        List<List<BasketV4Match>> roundMatches = new ArrayList<>();
        for (int roundNo = 1; roundNo <= roundCount; roundNo++) {
            int matchesInRound = teamCount / (1 << roundNo);
            List<BasketV4Match> currentRound = new ArrayList<>(matchesInRound);
            for (int slot = 0; slot < matchesInRound; slot++) {
                currentRound.add(newMatch(context, batch, roundNo, slot,
                        buildEliminationMatchNo(context.getDivision().getId(), context.getStage().getId(), roundNo, slot)));
            }
            roundMatches.add(currentRound);
        }

        List<BasketV4Match> firstRound = roundMatches.get(0);
        boolean seeded = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule())
                || PairingRuleEnum.HIGH_VS_LOW.getCode().equals(context.getStage().getPairingRule());
        BasketDivisionStageTeam[] pair = new BasketDivisionStageTeam[2];
        for (int slot = 0; slot < firstRound.size(); slot++) {
            BasketDivisionStageTeam home;
            BasketDivisionStageTeam away;
            if (seeded) {
                PlayoffBracketSupport.resolveAdjacentPair(orderedTeams, slot, pair);
                home = pair[0];
                away = pair[1];
            } else {
                home = orderedTeams.get(slot);
                away = orderedTeams.get(teamCount - 1 - slot);
            }
            BasketV4Match match = firstRound.get(slot);
            match.setHomeEventTeamId(home.getEventTeamId());
            match.setAwayEventTeamId(away.getEventTeamId());
            insertMatch(match);
        }

        for (int roundIndex = 1; roundIndex < roundCount; roundIndex++) {
            List<BasketV4Match> previousRound = roundMatches.get(roundIndex - 1);
            List<BasketV4Match> currentRound = roundMatches.get(roundIndex);
            for (int slot = 0; slot < currentRound.size(); slot++) {
                BasketV4Match match = currentRound.get(slot);
                match.setSourceMatchIdHome(previousRound.get(slot * 2).getId());
                match.setSourceMatchIdAway(previousRound.get(slot * 2 + 1).getId());
                insertMatch(match);
            }
        }
        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, roundCount);
    }

    public static boolean requiresConfirmedDraw(BasketDivisionStage stage) {
        String drawRule = stage.getDrawRule();
        return DrawRuleEnum.RANDOM_CONFIRM.getCode().equals(drawRule)
                || DrawRuleEnum.MANUAL_SLOT.getCode().equals(drawRule)
                || PairingRuleEnum.MANUAL.getCode().equals(stage.getPairingRule())
                || PairingRuleEnum.GROUP_CROSS.getCode().equals(stage.getPairingRule());
    }

    static boolean isSeriesElimination(BasketDivisionStage stage) {
        return MatchSeriesExecutionCapability.supportsScheduleGeneration(stage);
    }

    private List<BracketParticipant> toParticipants(List<BasketDivisionStageTeam> teams) {
        List<BracketParticipant> participants = new ArrayList<>();
        for (BasketDivisionStageTeam team : teams) {
            if (team.getEventTeamId() == null) {
                throw new ServiceException("阶段参赛队伍存在未绑定 eventTeamId 的记录，无法生成赛程");
            }
            BracketParticipant participant = new BracketParticipant();
            participant.setEventTeamId(team.getEventTeamId());
            participant.setSeedNo(team.getSeedNo());
            participant.setRankingNo(team.getSeedNo());
            participant.setSourceRank(team.getSourceRank());
            participant.setSourceType(team.getEntryType());
            participants.add(participant);
        }
        return participants;
    }

    private void validatePairingRule(BasketDivisionStage stage) {
        String pairingRule = stage.getPairingRule();
        if (pairingRule == null
                || PairingRuleEnum.NONE.getCode().equals(pairingRule)
                || PairingRuleEnum.HIGH_VS_LOW.getCode().equals(pairingRule)
                || PairingRuleEnum.RANDOM_DRAW.getCode().equals(pairingRule)
                || PairingRuleEnum.PLAYOFF_BRACKET.getCode().equals(pairingRule)
                || PairingRuleEnum.MANUAL.getCode().equals(pairingRule)
                || PairingRuleEnum.GROUP_CROSS.getCode().equals(pairingRule)) {
            return;
        }
        throw new ServiceException("当前版本单败淘汰仅支持 HIGH_VS_LOW / RANDOM_DRAW / PLAYOFF_BRACKET / MANUAL / GROUP_CROSS");
    }

    private String buildEliminationMatchNo(Long divisionId, Long stageId, int roundNo, int bracketSlot) {
        return String.format("V4-%d-E%d-R%d-S%02d", divisionId, stageId, roundNo, bracketSlot + 1);
    }
}
