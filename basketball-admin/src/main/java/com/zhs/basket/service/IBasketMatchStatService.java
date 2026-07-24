package com.zhs.basket.service;

import com.zhs.basket.req.MatchPlayerStatSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.req.MatchTeamStatSaveReq;
import com.zhs.basket.vo.MatchAuditLogVo;
import com.zhs.basket.vo.MatchPlayerStatVo;
import com.zhs.basket.vo.MatchTeamStatVo;

import java.util.List;

public interface IBasketMatchStatService {
    List<MatchTeamStatVo> selectTeamStats(Long matchId);
    void saveTeamStats(Long matchId, MatchTeamStatSaveReq req);
    void submitTeamStats(Long matchId);
    List<MatchPlayerStatVo> selectPlayerStats(Long matchId);
    void savePlayerStats(Long matchId, MatchPlayerStatSaveReq req);
    void submitPlayerStats(Long matchId);
    void confirmResult(Long matchId);
    void reopenResult(Long matchId, MatchReopenReq req);
    MatchAuditLogVo selectAuditLog(Long matchId);
}
