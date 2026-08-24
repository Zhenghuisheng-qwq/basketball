package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import com.zhs.basket.support.v4.stage.StageExecutionContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * NATURAL_RESOLUTION: QF winner fills SF slot. Allowed after PUBLISHED.
 * Does not mutate pairing / redraw. Does not clear schedule.
 */
@Component
public class WinnerOfSeriesAdvanceSupport {

    public static final String MSG_DOWNSTREAM_STARTED =
            "下游系列赛已经开始或产生正式结果，不能直接修改上游晋级结果，请进入赛事管理员仲裁流程。";

    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private MatchSeriesSupport matchSeriesSupport;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private com.zhs.basket.support.v4.EventTeamParticipationGuard eventTeamParticipationGuard;

    public void advanceIfCompleted(BasketMatchSeries completed) {
        rewireAfterSeriesResult(completed);
    }

    public void rewireAfterSeriesResult(BasketMatchSeries source) {
        if (source == null || source.getId() == null) {
            return;
        }
        List<BasketMatchSeries> next = matchSeriesMapper.selectBySourceSeriesId(source.getId());
        if (next == null || next.isEmpty()) {
            return;
        }
        boolean completed = MatchSeriesStatusEnum.COMPLETED.getCode().equals(source.getSeriesStatus());
        Long winner = completed ? positive(source.getWinnerEventTeamId()) : null;
        if (winner != null && eventTeamParticipationGuard != null && eventTeamParticipationGuard.isDisqualified(winner)) {
            winner = null;
        }
        for (BasketMatchSeries target : next) {
            applyOrClear(source.getId(), winner, target);
        }
    }

    public void assertDownstreamNotStarted(BasketMatchSeries source) {
        if (source == null || source.getId() == null) {
            return;
        }
        List<BasketMatchSeries> next = matchSeriesMapper.selectBySourceSeriesId(source.getId());
        if (next == null) {
            return;
        }
        for (BasketMatchSeries target : next) {
            if (isDownstreamStarted(target)) {
                throw new ServiceException(MSG_DOWNSTREAM_STARTED);
            }
        }
    }

    private void applyOrClear(Long sourceSeriesId, Long winner, BasketMatchSeries target) {
        boolean home = Objects.equals(sourceSeriesId, target.getSourceSeriesIdHome());
        boolean away = Objects.equals(sourceSeriesId, target.getSourceSeriesIdAway());
        if (!home && !away) {
            return;
        }
        Long current = home ? positive(target.getHomeEventTeamId()) : positive(target.getAwayEventTeamId());
        if (winner == null) {
            if (current == null) {
                return;
            }
            assertSlotMutable(target);
            clearSlot(target, home);
            return;
        }
        if (current != null && current.equals(winner)) {
            ensureChildGames(target);
            return;
        }
        if (current != null) {
            assertSlotMutable(target);
        }
        if (home) {
            target.setHomeEventTeamId(winner);
            if (target.getHomeSourceType() == null || target.getHomeSourceType().isBlank()) {
                target.setHomeSourceType(ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode());
            }
        }
        if (away) {
            target.setAwayEventTeamId(winner);
            if (target.getAwaySourceType() == null || target.getAwaySourceType().isBlank()) {
                target.setAwaySourceType(ParticipantSourceTypeEnum.WINNER_OF_SERIES.getCode());
            }
        }
        target.setUpdateTime(new Date());
        if (matchSeriesMapper.updatePairing(target) != 1) {
            throw new ServiceException("胜者晋级写入下一轮失败");
        }
        ensureChildGames(target);
    }

    private void clearSlot(BasketMatchSeries target, boolean home) {
        if (home) {
            target.setHomeEventTeamId(0L);
        } else {
            target.setAwayEventTeamId(0L);
        }
        if (MatchSeriesStatusEnum.READY.getCode().equals(target.getSeriesStatus())
                || MatchSeriesStatusEnum.RUNNING.getCode().equals(target.getSeriesStatus())) {
            target.setSeriesStatus(MatchSeriesStatusEnum.PENDING.getCode());
        }
        target.setUpdateTime(new Date());
        if (matchSeriesMapper.updatePairing(target) != 1) {
            throw new ServiceException("撤销胜者晋级失败");
        }
        List<BasketV4Match> games = matchMapper.selectBySeriesId(target.getId());
        if (games == null) {
            return;
        }
        for (BasketV4Match game : games) {
            if (home) {
                game.setHomeEventTeamId(0L);
            } else {
                game.setAwayEventTeamId(0L);
            }
            game.setUpdateTime(new Date());
            matchMapper.updateById(game);
        }
    }

    private void assertSlotMutable(BasketMatchSeries target) {
        if (isDownstreamStarted(target)) {
            throw new ServiceException(MSG_DOWNSTREAM_STARTED);
        }
    }

    public boolean isDownstreamStarted(BasketMatchSeries target) {
        if (target == null) {
            return false;
        }
        String status = target.getSeriesStatus();
        if (MatchSeriesStatusEnum.RUNNING.getCode().equals(status)
                || MatchSeriesStatusEnum.COMPLETED.getCode().equals(status)) {
            return true;
        }
        List<BasketV4Match> games = matchMapper.selectBySeriesId(target.getId());
        if (games == null) {
            return false;
        }
        for (BasketV4Match game : games) {
            String matchStatus = game.getMatchStatus();
            if (MatchStatusEnum.PLAYING.getCode().equals(matchStatus)
                    || MatchStatusEnum.FINISHED.getCode().equals(matchStatus)
                    || MatchStatusEnum.FORFEITED.getCode().equals(matchStatus)) {
                return true;
            }
        }
        return false;
    }

    private void ensureChildGames(BasketMatchSeries target) {
        Long homeId = positive(target.getHomeEventTeamId());
        Long awayId = positive(target.getAwayEventTeamId());
        if (homeId == null || awayId == null) {
            return;
        }
        BasketDivision division = divisionMapper.selectById(target.getDivisionId());
        BasketDivisionStage stage = stageMapper.selectById(target.getStageId());
        BasketScheduleBatch batch = scheduleBatchMapper.selectById(target.getScheduleBatchId());
        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        matchSeriesSupport.ensureGames(context, batch, target, homeId, awayId);
    }

    private Long positive(Long id) {
        return id == null || id <= 0 ? null : id;
    }
}
