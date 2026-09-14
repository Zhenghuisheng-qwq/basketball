package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.SeriesResolutionTypeEnum;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.v4.MatchSeriesGameInput;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.basket.support.v4.MatchSeriesResultCalculator;
import com.zhs.basket.support.v4.MatchSeriesResultSnapshot;
import com.zhs.basket.support.v4.V4PublicReadCachePublisher;
import com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.uuid.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketMatchSeriesServiceImpl implements IBasketMatchSeriesService {

    private static final int NOT_DELETED = 0;

    @Autowired
    private BasketMatchSeriesMapper seriesMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private MatchSeriesQualificationSyncService qualificationSyncService;
    @Autowired
    @Lazy
    private IDivisionStageExecutionApplicationService stageExecutionService;
    @Autowired
    private WinnerOfSeriesAdvanceSupport winnerOfSeriesAdvanceSupport;
    @Autowired
    private V4PublicReadCachePublisher publicReadCachePublisher;

    @Override
    public BasketMatchSeries getById(Long id) {
        return requireSeries(id);
    }

    @Override
    public List<BasketMatchSeries> listByStageId(Long stageId) {
        if (stageId == null) {
            throw new ServiceException("stageId 不能为空");
        }
        return seriesMapper.selectByStageId(stageId);
    }

    @Override
    public List<BasketMatchSeries> listByScheduleBatchId(Long scheduleBatchId) {
        if (scheduleBatchId == null) {
            throw new ServiceException("scheduleBatchId 不能为空");
        }
        return seriesMapper.selectByScheduleBatchId(scheduleBatchId);
    }

    @Override
    public List<BasketMatchSeries> listBySourceSeriesId(Long sourceSeriesId) {
        if (sourceSeriesId == null) {
            throw new ServiceException("sourceSeriesId 不能为空");
        }
        return seriesMapper.selectBySourceSeriesId(sourceSeriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeries(BasketMatchSeries series) {
        if (series == null) {
            throw new ServiceException("系列赛不能为空");
        }
        MatchSeriesResultCalculator.requiredWins(series.getBestOf());
        if (series.getHomeEventTeamId() == null || series.getAwayEventTeamId() == null) {
            throw new ServiceException("系列赛双方球队不能为空");
        }
        if (series.getHomeEventTeamId().equals(series.getAwayEventTeamId())) {
            throw new ServiceException("系列赛主客队不能相同");
        }
        if (series.getDivisionId() == null || series.getStageId() == null) {
            throw new ServiceException("divisionId 与 stageId 不能为空");
        }
        if (series.getRoundNo() == null || series.getBracketSlot() == null) {
            throw new ServiceException("roundNo 与 bracketSlot 不能为空");
        }

        Date now = new Date();
        if (series.getSeriesCode() == null || series.getSeriesCode().isEmpty()) {
            series.setSeriesCode("MS-" + IdUtils.fastSimpleUUID());
        }
        if (series.getHomeWinCount() == null) {
            series.setHomeWinCount(0);
        }
        if (series.getAwayWinCount() == null) {
            series.setAwayWinCount(0);
        }
        if (series.getSeriesStatus() == null) {
            series.setSeriesStatus(MatchSeriesStatusEnum.PENDING.getCode());
        }
        series.setWinnerEventTeamId(null);
        series.setCreateTime(now);
        series.setUpdateTime(now);
        series.setIsDelete(NOT_DELETED);

        if (seriesMapper.insert(series) != 1) {
            throw new ServiceException("创建系列赛失败");
        }
        return series.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSeriesResult(Long seriesId, int homeWinCount, int awayWinCount,
                                   Long winnerEventTeamId, String seriesStatus) {
        BasketMatchSeries series = requireSeries(seriesId);
        validateResultAgainstBestOf(series.getBestOf(), homeWinCount, awayWinCount, winnerEventTeamId, seriesStatus);

        BasketMatchSeries update = new BasketMatchSeries();
        update.setId(seriesId);
        update.setHomeWinCount(homeWinCount);
        update.setAwayWinCount(awayWinCount);
        update.setWinnerEventTeamId(winnerEventTeamId);
        update.setSeriesStatus(seriesStatus);
        if (seriesMapper.updateResult(update) != 1) {
            throw new ServiceException("更新系列赛结果失败");
        }
        publicReadCachePublisher.seriesMutation(series.getDivisionId(), null, series.getStageId(),
                seriesId, series.getHomeEventTeamId(), series.getAwayEventTeamId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildSeriesResult(Long seriesId) {
        BasketMatchSeries locked = seriesMapper.selectByIdForUpdate(seriesId);
        if (locked == null) {
            throw new ServiceException("系列赛不存在");
        }
        if (SeriesResolutionTypeEnum.FORFEIT.getCode().equals(locked.getResolutionType())
                || SeriesResolutionTypeEnum.MANUAL.getCode().equals(locked.getResolutionType())) {
            return;
        }

        String previousStatus = locked.getSeriesStatus();
        Long previousWinner = locked.getWinnerEventTeamId();

        List<BasketV4Match> matches = matchMapper.selectEffectiveBySeriesId(seriesId);
        List<MatchSeriesGameInput> inputs = matches.stream().map(m -> {
            MatchSeriesGameInput input = new MatchSeriesGameInput();
            input.setMatchId(m.getId());
            input.setSeriesId(m.getSeriesId());
            input.setSeriesGameNo(m.getSeriesGameNo());
            input.setHomeEventTeamId(m.getHomeEventTeamId());
            input.setAwayEventTeamId(m.getAwayEventTeamId());
            input.setWinnerEventTeamId(m.getWinnerEventTeamId());
            input.setMatchStatus(m.getMatchStatus());
            return input;
        }).toList();

        MatchSeriesResultSnapshot snapshot = MatchSeriesResultCalculator.calculate(
                locked.getBestOf(),
                locked.getHomeEventTeamId(),
                locked.getAwayEventTeamId(),
                inputs);

        String newStatus = resolveStatus(locked.getSeriesStatus(), snapshot);
        updateSeriesResult(
                seriesId,
                snapshot.getHomeWinCount(),
                snapshot.getAwayWinCount(),
                snapshot.getWinnerEventTeamId(),
                newStatus);

        locked.setSeriesStatus(newStatus);
        locked.setWinnerEventTeamId(snapshot.getWinnerEventTeamId());
        winnerOfSeriesAdvanceSupport.rewireAfterSeriesResult(locked);
        qualificationSyncService.syncAfterSeriesRebuild(locked, previousStatus, previousWinner, snapshot);
        winnerOfSeriesAdvanceSupport.rewireAfterSeriesResult(locked);

        if (locked.getStageId() != null) {
            stageExecutionService.checkStageCompletion(locked.getStageId());
        }
    }

    private String resolveStatus(String currentStatus, MatchSeriesResultSnapshot snapshot) {
        if (MatchSeriesStatusEnum.CANCELLED.getCode().equals(currentStatus)) {
            return currentStatus;
        }
        if (snapshot.isCompleted()) {
            return MatchSeriesStatusEnum.COMPLETED.getCode();
        }
        if (snapshot.getHomeWinCount() > 0 || snapshot.getAwayWinCount() > 0) {
            return MatchSeriesStatusEnum.RUNNING.getCode();
        }
        if (MatchSeriesStatusEnum.READY.getCode().equals(currentStatus)) {
            return MatchSeriesStatusEnum.READY.getCode();
        }
        return MatchSeriesStatusEnum.PENDING.getCode();
    }

    private void validateResultAgainstBestOf(int bestOf, int homeWinCount, int awayWinCount,
                                             Long winnerEventTeamId, String seriesStatus) {
        int required = MatchSeriesResultCalculator.requiredWins(bestOf);
        if (homeWinCount < 0 || awayWinCount < 0) {
            throw new ServiceException("胜场数不能为负");
        }
        if (homeWinCount + awayWinCount > bestOf) {
            throw new ServiceException("胜场总数不能超过 best_of");
        }
        if (homeWinCount >= required && awayWinCount >= required) {
            throw new ServiceException("双方不能同时达到胜场阈值");
        }

        boolean completed = MatchSeriesStatusEnum.COMPLETED.getCode().equals(seriesStatus);
        if (completed) {
            if (winnerEventTeamId == null) {
                throw new ServiceException("COMPLETED 系列赛必须有 winner");
            }
            if (homeWinCount < required && awayWinCount < required) {
                throw new ServiceException("COMPLETED 系列赛胜场未达阈值");
            }
        } else if (winnerEventTeamId != null
                && !MatchSeriesStatusEnum.CANCELLED.getCode().equals(seriesStatus)) {
            throw new ServiceException("未完成系列赛不能有 winner");
        }
    }

    private BasketMatchSeries requireSeries(Long id) {
        if (id == null) {
            throw new ServiceException("系列赛 ID 不能为空");
        }
        BasketMatchSeries series = seriesMapper.selectById(id);
        if (series == null) {
            throw new ServiceException("系列赛不存在");
        }
        return series;
    }
}
