package com.zhs.basket.service;

import com.zhs.basket.req.RoundCreateReq;
import com.zhs.basket.req.RoundUpdateReq;
import com.zhs.basket.vo.RoundDetailVo;
import com.zhs.basket.vo.RoundListVo;
import java.util.List;

public interface IBasketTournamentRoundService {
    List<RoundListVo> selectByStageId(Long stageId);
    RoundDetailVo selectById(Long roundId);
    Long create(Long stageId, RoundCreateReq req);
    void update(Long roundId, RoundUpdateReq req);
    void activate(Long roundId);
    void finish(Long roundId);
    void cancel(Long roundId);
    void delete(Long roundId);
}
