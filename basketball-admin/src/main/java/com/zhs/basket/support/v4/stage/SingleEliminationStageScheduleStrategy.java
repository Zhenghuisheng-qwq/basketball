package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.ScheduleGenerationStrategyEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单败淘汰阶段赛程策略。
 */
@Component
public class SingleEliminationStageScheduleStrategy extends AbstractStageScheduleStrategy {

    private static final String RULE_CODE = "SINGLE_ELIMINATION_V1";

    @Override
    public boolean supports(BasketDivisionStage stage) {
        return CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(stage.getCompetitionFormat());
    }

    @Override
    public void validate(StageExecutionContext context) {
        requireContext(context);
        validateBestOfOne(context.getStage());
        validateTeamsPresent(context);

        int teamCount = context.getStageTeams().size();
        if (!StageScheduleMathSupport.isPowerOfTwo(teamCount)) {
            throw new ServiceException("单败淘汰阶段参赛队伍数必须为 2 的幂次（当前 " + teamCount + " 支），不支持轮空");
        }

        BasketDivisionStage stage = context.getStage();
        String pairingRule = stage.getPairingRule();
        if (pairingRule != null
                && !PairingRuleEnum.NONE.getCode().equals(pairingRule)
                && !PairingRuleEnum.HIGH_VS_LOW.getCode().equals(pairingRule)) {
            throw new ServiceException("当前版本单败淘汰仅支持 HIGH_VS_LOW 对阵规则");
        }
    }

    @Override
    public StageSchedulePreviewVo preview(StageExecutionContext context) {
        validate(context);
        int teamCount = context.getStageTeams().size();
        StageSchedulePreviewVo preview = new StageSchedulePreviewVo();
        preview.setTeamCount(teamCount);
        preview.setTotalMatches(StageScheduleMathSupport.eliminationMatchCount(teamCount));
        preview.setRoundCount(StageScheduleMathSupport.eliminationRoundCount(teamCount));
        preview.setMatchesPerTeam(StageScheduleMathSupport.eliminationRoundCount(teamCount));
        preview.setHasBye(false);
        return preview;
    }

    @Override
    public ScheduleGenerationResult generate(StageExecutionContext context) {
        validate(context);
        List<BasketDivisionStageTeam> seededTeams = context.getStageTeams().stream()
                .sorted(Comparator
                        .comparing(BasketDivisionStageTeam::getSeedNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(BasketDivisionStageTeam::getEventTeamId))
                .toList();

        int teamCount = seededTeams.size();
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
        for (int slot = 0; slot < firstRound.size(); slot++) {
            int highIndex = slot;
            int lowIndex = teamCount - 1 - slot;
            BasketDivisionStageTeam home = seededTeams.get(highIndex);
            BasketDivisionStageTeam away = seededTeams.get(lowIndex);
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
                BasketV4Match homeSource = previousRound.get(slot * 2);
                BasketV4Match awaySource = previousRound.get(slot * 2 + 1);
                match.setSourceMatchIdHome(homeSource.getId());
                match.setSourceMatchIdAway(awaySource.getId());
                insertMatch(match);
            }
        }

        return new ScheduleGenerationResult(batch.getId(), expectedMatchCount, expectedMatchCount, roundCount);
    }

    private String buildEliminationMatchNo(Long divisionId, Long stageId, int roundNo, int bracketSlot) {
        return String.format("V4-%d-E%d-R%d-S%02d", divisionId, stageId, roundNo, bracketSlot + 1);
    }
}
