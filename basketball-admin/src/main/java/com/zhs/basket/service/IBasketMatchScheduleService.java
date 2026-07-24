package com.zhs.basket.service;

import com.zhs.basket.query.MatchQuery;
import com.zhs.basket.req.*;
import com.zhs.basket.vo.MatchDetailVo;
import com.zhs.basket.vo.MatchListVo;
import com.zhs.basket.vo.VenueSimpleVo;

import java.util.List;

public interface IBasketMatchScheduleService {
    List<MatchListVo> selectList(MatchQuery query);
    MatchDetailVo selectById(Long matchId);
    List<VenueSimpleVo> selectVenues();
    Long create(MatchCreateReq req);
    void update(Long matchId, MatchUpdateReq req);
    void schedule(Long matchId, MatchScheduleReq req);
    void postpone(Long matchId, MatchPostponeReq req);
    void cancel(Long matchId, MatchCancelReq req);
    void start(Long matchId);
    void delete(Long matchId);
}
