package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketPlayerStat;
import com.zhs.basket.domain.v4.BasketRank;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.domain.v4.BasketTeamStat;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketRankMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
import com.zhs.basket.mapper.v4.BasketTeamStatMapper;
import com.zhs.basket.query.v4.BasketV4StatsQuery;
import com.zhs.basket.service.v4.IBasketV4StatsQueryService;
import com.zhs.basket.vo.v4.*;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BasketV4StatsQueryServiceImpl implements IBasketV4StatsQueryService {

    @Autowired
    private BasketStandingMapper standingMapper;
    @Autowired
    private BasketTeamStatMapper teamStatMapper;
    @Autowired
    private BasketPlayerStatMapper playerStatMapper;
    @Autowired
    private BasketRankMapper rankMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketEventPlayerMapper eventPlayerMapper;

    @Override
    public List<BasketStandingListVo> listStanding(BasketV4StatsQuery query) {
        requireDivisionId(query);
        List<BasketStandingListVo> result = new ArrayList<>();
        for (BasketStanding row : standingMapper.selectByDivisionId(query.getDivisionId())) {
            BasketStandingListVo vo = new BasketStandingListVo();
            vo.setEventTeamId(row.getEventTeamId());
            vo.setRankNo(row.getRankNo());
            vo.setWinCount(row.getWinCount());
            vo.setLoseCount(row.getLoseCount());
            vo.setPointFor(row.getPointFor());
            vo.setPointAgainst(row.getPointAgainst());
            vo.setPointDiff(row.getPointDiff());
            vo.setStandingPoints(row.getStandingPoints());
            BasketEventTeam team = eventTeamMapper.selectById(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BasketTeamStatListVo> listTeamStat(BasketV4StatsQuery query) {
        requireDivisionId(query);
        List<BasketTeamStatListVo> result = new ArrayList<>();
        for (BasketTeamStat row : teamStatMapper.selectByDivisionId(query.getDivisionId())) {
            BasketTeamStatListVo vo = new BasketTeamStatListVo();
            vo.setEventTeamId(row.getEventTeamId());
            vo.setMatchCount(row.getMatchCount());
            vo.setWinCount(row.getWinCount());
            vo.setLoseCount(row.getLoseCount());
            vo.setPointsFor(row.getPointsFor());
            vo.setPointsAgainst(row.getPointsAgainst());
            vo.setAvgPointsFor(row.getAvgPointsFor());
            vo.setAvgPointsAgainst(row.getAvgPointsAgainst());
            vo.setRebound(row.getRebound());
            vo.setAssist(row.getAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            BasketEventTeam team = eventTeamMapper.selectById(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BasketPlayerStatListVo> listPlayerStat(BasketV4StatsQuery query) {
        requireDivisionId(query);
        List<BasketPlayerStatListVo> result = new ArrayList<>();
        for (BasketPlayerStat row : playerStatMapper.selectByDivisionId(query.getDivisionId())) {
            BasketPlayerStatListVo vo = new BasketPlayerStatListVo();
            vo.setEventPlayerId(row.getEventPlayerId());
            vo.setEventTeamId(row.getEventTeamId());
            vo.setMatchCount(row.getMatchCount());
            vo.setPoints(row.getPoints());
            vo.setAvgPoints(row.getAvgPoints());
            vo.setRebound(row.getRebound());
            vo.setAvgRebound(row.getAvgRebound());
            vo.setAssist(row.getAssist());
            vo.setAvgAssist(row.getAvgAssist());
            vo.setSteal(row.getSteal());
            vo.setBlock(row.getBlock());
            vo.setTurnover(row.getTurnover());
            BasketEventPlayer player = eventPlayerMapper.selectById(row.getEventPlayerId());
            if (player != null) {
                vo.setPlayerName(player.getPlayerNameSnapshot());
            }
            BasketEventTeam team = eventTeamMapper.selectById(row.getEventTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamNameSnapshot());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BasketRankListVo> listRank(BasketV4StatsQuery query) {
        requireDivisionId(query);
        List<BasketRankListVo> result = new ArrayList<>();
        for (BasketRank row : rankMapper.selectByDivisionId(query.getDivisionId())) {
            if (query.getRankType() != null && !query.getRankType().equals(row.getRankType())) {
                continue;
            }
            BasketRankListVo vo = new BasketRankListVo();
            vo.setRankType(row.getRankType());
            vo.setRankNo(row.getRankNo());
            vo.setTargetType(row.getTargetType());
            vo.setEventTeamId(row.getEventTeamId());
            vo.setEventPlayerId(row.getEventPlayerId());
            vo.setRankValue(row.getRankValue());
            if (row.getEventTeamId() != null) {
                BasketEventTeam team = eventTeamMapper.selectById(row.getEventTeamId());
                if (team != null) {
                    vo.setTeamName(team.getTeamNameSnapshot());
                }
            }
            if (row.getEventPlayerId() != null) {
                BasketEventPlayer player = eventPlayerMapper.selectById(row.getEventPlayerId());
                if (player != null) {
                    vo.setPlayerName(player.getPlayerNameSnapshot());
                }
            }
            result.add(vo);
        }
        return result;
    }

    private void requireDivisionId(BasketV4StatsQuery query) {
        if (query == null || query.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
    }
}
