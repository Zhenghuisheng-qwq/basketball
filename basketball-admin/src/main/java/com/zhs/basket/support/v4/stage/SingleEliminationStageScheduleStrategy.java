package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.basket.support.v4.ByeQualificationService;
import com.zhs.basket.support.v4.MatchSeriesExecutionCapability;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 单败淘汰阶段赛程策略。
 */
@Component
public class SingleEliminationStageScheduleStrategy extends AbstractStageScheduleStrategy {

    private static final String RULE_CODE = "SINGLE_ELIMINATION_V1";

    @Autowired
    private MatchSeriesEliminationScheduleGenerator seriesScheduleGenerator;
    @Autowired
    private ByeQualificationService byeQualificationService;

    /** 仅测试注入固定种子；生产默认 null（使用无种子 shuffle）。 */
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

        BasketDivisionStage stage = context.getStage();
        if (isSeriesElimination(stage)) {
            validateSeriesElimination(context);
            return;
        }

        validateBestOfOne(stage);
        validateBo1Elimination(context);
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        BasketDivisionStage stage = context.getStage();
        int teamCount = context.getStageTeams().size();

        if (isSeriesElimination(stage)) {
            int bestOf = stage.getBestOf();
            int seriesCount = EliminationByeSupport.pairingSlotCount(teamCount);
            int maxGameCount = seriesCount * bestOf;
            boolean hasBye = EliminationByeSupport.allowsBye(stage, teamCount);
            StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
            preview.setTeamCount(teamCount);
            preview.setSeriesCount(seriesCount);
            preview.setMaxGameCount(maxGameCount);
            preview.setTotalMatches(maxGameCount);
            preview.setRoundCount(1);
            preview.setMatchesPerTeam(bestOf);
            preview.setHasBye(hasBye);
            if (hasBye) {
                preview.getWarnings().add("当前轮次存在轮空晋级（最高 seed 轮空）");
            }
            return preview;
        }

        StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
        preview.setTeamCount(teamCount);
        if (EliminationStageSupport.isSingleRoundNonFinalStage(stage)) {
            int matchCount = EliminationByeSupport.pairingSlotCount(teamCount);
            preview.setTotalMatches(matchCount);
            preview.setRoundCount(1);
            preview.setMatchesPerTeam(1);
            preview.setHasBye(EliminationByeSupport.allowsBye(stage, teamCount));
            if (Boolean.TRUE.equals(preview.getHasBye())) {
                preview.getWarnings().add("当前轮次存在轮空晋级（最高 seed 轮空）");
            }
        } else {
            preview.setTotalMatches(StageScheduleMathSupport.eliminationMatchCount(teamCount));
            preview.setRoundCount(StageScheduleMathSupport.eliminationRoundCount(teamCount));
            preview.setMatchesPerTeam(StageScheduleMathSupport.eliminationRoundCount(teamCount));
        }
        if (preview.getHasBye() == null) {
            preview.setHasBye(false);
        }
        return preview;
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        if (isSeriesElimination(context.getStage())) {
            seriesScheduleGenerator.random = random;
            return seriesScheduleGenerator.generate(context);
        }
        return generateBo1(context);
    }

    private ScheduleGenerationResult generateBo1(StageExecutionContext context) {
        BasketDivisionStage stage = context.getStage();
        List<BasketDivisionStageTeam> orderedTeams = PlayoffBracketSupport.isPlayoffBracket(stage.getPairingRule())
                ? PlayoffBracketSupport.orderTeams(context.getStageTeams())
                : orderTeamsForPairing(context);
        int teamCount = orderedTeams.size();

        if (EliminationStageSupport.isSingleRoundNonFinalStage(stage)) {
            return generateSingleRoundBo1(context, orderedTeams, teamCount);
        }
        return generateFullBracketBo1(context, orderedTeams, teamCount);
    }

    private ScheduleGenerationResult generateSingleRoundBo1(StageExecutionContext context,
                                                              List<BasketDivisionStageTeam> orderedTeams,
                                                              int teamCount) {
        boolean playoff = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule());
        Long byeTeamId = (!playoff && EliminationByeSupport.allowsBye(context.getStage(), teamCount))
                ? EliminationByeSupport.resolveByeEventTeamId(orderedTeams) : null;
        List<BasketDivisionStageTeam> pairingTeams = byeTeamId != null
                ? EliminationByeSupport.pairingTeams(orderedTeams, byeTeamId) : orderedTeams;
        int seriesCount = EliminationByeSupport.pairingSlotCount(teamCount);

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE,
                teamCount,
                seriesCount,
                null);

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

    private ScheduleGenerationResult generateFullBracketBo1(StageExecutionContext context,
                                                          List<BasketDivisionStageTeam> orderedTeams,
                                                          int teamCount) {
        int roundCount = StageScheduleMathSupport.eliminationRoundCount(teamCount);
        int expectedMatchCount = StageScheduleMathSupport.eliminationMatchCount(teamCount);

        BasketScheduleBatch batch = createBatch(context,
                ScheduleGenerationStrategyEnum.SINGLE_ELIMINATION,
                RULE_CODE,
                teamCount,
                expectedMatchCount,
                null);

        List<List<BasketV4Match>> roundMatches = new ArrayList<>();
        for (int roundNo = 1; roundNo <= roundCount; roundNo++) {
            int matchesInRound = teamCount / (1 << roundNo);
            List<BasketV4Match> currentRound = new ArrayList<>(matchesInRound);
            for (int slot = 0; slot < matchesInRound; slot++) {
                BasketV4Match match = newMatch(context, batch, roundNo, slot,
                        buildEliminationMatchNo(context.getDivision().getId(), context.getStage().getId(), roundNo, slot));
                currentRound.add(match);
            }
            roundMatches.add(currentRound);
        }

        List<BasketV4Match> firstRound = roundMatches.get(0);
        boolean playoff = PlayoffBracketSupport.isPlayoffBracket(context.getStage().getPairingRule());
        BasketDivisionStageTeam[] pair = new BasketDivisionStageTeam[2];
        for (int slot = 0; slot < firstRound.size(); slot++) {
            BasketDivisionStageTeam home;
            BasketDivisionStageTeam away;
            if (playoff) {
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

    private List<BasketDivisionStageTeam> orderTeamsForPairing(StageExecutionContext context) {
        return EliminationByeSupport.orderTeamsForPairing(
                context.getStageTeams(), context.getStage().getPairingRule(), random);
    }

    private void validateBo1Elimination(StageExecutionContext context) {
        BasketDivisionStage stage = context.getStage();
        validatePairingRule(stage);

        int teamCount = context.getStageTeams().size();
        if (EliminationStageSupport.isSingleRoundNonFinalStage(stage)) {
            return;
        }

        if (!StageScheduleMathSupport.isPowerOfTwo(teamCount)) {
            throw new ServiceException("单败淘汰阶段参赛队伍数必须为 2 的幂次（当前 " + teamCount + " 支），不支持轮空");
        }
    }

    private void validateSeriesElimination(StageExecutionContext context) {
        BasketDivisionStage stage = context.getStage();
        validatePairingRule(stage);

        int teamCount = context.getStageTeams().size();
        if (EliminationStageSupport.isFinalEliminationStage(stage)) {
            if (teamCount > 2) {
                throw new ServiceException(
                        "当前版本 BO3/BO5 最终淘汰阶段仅支持 2 支队伍，总淘汰树多轮系列赛将在后续能力中扩展。");
            }
        }
    }

    private void validatePairingRule(BasketDivisionStage stage) {
        String pairingRule = stage.getPairingRule();
        if (pairingRule == null
                || PairingRuleEnum.NONE.getCode().equals(pairingRule)
                || PairingRuleEnum.HIGH_VS_LOW.getCode().equals(pairingRule)
                || PairingRuleEnum.RANDOM_DRAW.getCode().equals(pairingRule)
                || PairingRuleEnum.PLAYOFF_BRACKET.getCode().equals(pairingRule)) {
            return;
        }
        throw new ServiceException("当前版本单败淘汰仅支持 HIGH_VS_LOW / RANDOM_DRAW / PLAYOFF_BRACKET 对阵规则");
    }

    static boolean isSeriesElimination(BasketDivisionStage stage) {
        return MatchSeriesExecutionCapability.supportsScheduleGeneration(stage);
    }

    static boolean isFinalEliminationStage(BasketDivisionStage stage) {
        return EliminationStageSupport.isFinalEliminationStage(stage);
    }

    private String buildEliminationMatchNo(Long divisionId, Long stageId, int roundNo, int bracketSlot) {
        return String.format("V4-%d-E%d-R%d-S%02d", divisionId, stageId, roundNo, bracketSlot + 1);
    }
}
