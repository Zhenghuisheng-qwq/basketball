package com.zhs.basket.service;

import com.zhs.basket.req.MatchLineupSaveReq;
import com.zhs.basket.vo.AvailableMatchPlayerVo;
import com.zhs.basket.vo.MatchLineupVo;

import java.util.List;

public interface IBasketMatchLineupService {
    MatchLineupVo selectLineup(Long matchId, Long teamId);
    List<AvailableMatchPlayerVo> selectAvailablePlayers(Long matchId, Long teamId);
    void saveLineup(Long matchId, Long teamId, MatchLineupSaveReq req);
    void confirmLineup(Long matchId, Long teamId);
}
