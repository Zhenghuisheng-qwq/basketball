package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import com.zhs.basket.support.v4.stage.StageExecutionContext;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * NATURAL_RESOLUTION: QF winner fills SF slot. Allowed after PUBLISHED.
 * Does not mutate pairing / redraw. Does not clear schedule.
 */
@Component
public class WinnerOfSeriesAdvanceSupport {

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

    public void advanceIfCompleted(BasketMatchSeries completed) {
        if (completed == null || completed.getId() == null) {
            return;
        }
        if (!MatchSeriesStatusEnum.COMPLETED.getCode().equals(completed.getSeriesStatus())) {
            return;
        }
        Long winner = completed.getWinnerEventTeamId();
        if (winner == null || winner <= 0) {
            return;
        }
        List<BasketMatchSeries> next = matchSeriesMapper.selectBySourceSeriesId(completed.getId());
        if (next == null || next.isEmpty()) {
            return;
        }
        for (BasketMatchSeries target : next) {
            applyWinner(completed.getId(), winner, target);
        }
    }

    private void applyWinner(Long sourceSeriesId, Long winner, BasketMatchSeries target) {
        boolean home = Objects.equals(sourceSeriesId, target.getSourceSeriesIdHome());
        boolean away = Objects.equals(sourceSeriesId, target.getSourceSeriesIdAway());
        if (!home && !away) {
            return;
        }
        Long current = home ? target.getHomeEventTeamId() : target.getAwayEventTeamId();
        if (current != null && current > 0 && !current.equals(winner)) {
            throw new ServiceException("已解析的胜者不能改绑到另一支球队");
        }
        if (current != null && current.equals(winner)) {
            ensureChildGames(target);
            return;
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
        if (matchSeriesMapper.updateById(target) != 1) {
            throw new ServiceException("胜者晋级写入下一轮失败");
        }
        ensureChildGames(target);
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
