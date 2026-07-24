package com.zhs.basket.service;

import com.zhs.basket.req.MatchForfeitReq;
import com.zhs.basket.req.MatchPeriodScoreSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.vo.MatchScoreVo;

public interface IBasketMatchScoreService {
    MatchScoreVo selectScore(Long matchId);
    void savePeriodScores(Long matchId, MatchPeriodScoreSaveReq req);
    void submitScore(Long matchId);
    void confirmScore(Long matchId);
    void reopenScore(Long matchId, MatchReopenReq req);
    void forfeit(Long matchId, MatchForfeitReq req);
}
