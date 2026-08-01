package com.zhs.basket.service;

import com.zhs.basket.query.ScheduleMatchQuery;
import com.zhs.basket.req.ScheduleMatchCreateReq;
import com.zhs.basket.req.ScheduleMatchUpdateReq;
import com.zhs.basket.req.ScheduleScoreSaveReq;
import com.zhs.basket.vo.ScheduleMatchDetailVo;
import com.zhs.basket.vo.ScheduleMatchListVo;

import java.util.List;

public interface IBasketScheduleMatchAdminService {
    List<ScheduleMatchListVo> list(ScheduleMatchQuery query);
    ScheduleMatchDetailVo getDetail(Long matchId);
    Long createManual(ScheduleMatchCreateReq req);
    void update(Long matchId, ScheduleMatchUpdateReq req);
    void swapHomeAway(Long matchId, String reason);
    void deleteDraftMatch(Long matchId, String reason);
    void saveScore(Long matchId, ScheduleScoreSaveReq req);
}
