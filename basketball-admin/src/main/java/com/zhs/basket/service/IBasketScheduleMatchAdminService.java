package com.zhs.basket.service;

import com.zhs.basket.query.ScheduleMatchQuery;
import com.zhs.basket.req.ScheduleMatchCreateReq;
import com.zhs.basket.req.ScheduleMatchUpdateReq;
import com.zhs.basket.req.ScheduleScoreSaveReq;
import com.zhs.basket.vo.ScheduleMatchListVo;

import java.util.List;

public interface IBasketScheduleMatchAdminService {
    List<ScheduleMatchListVo> list(ScheduleMatchQuery query);
    Long createManual(ScheduleMatchCreateReq req);
    void update(Long matchId, ScheduleMatchUpdateReq req);
    void saveScore(Long matchId, ScheduleScoreSaveReq req);
}
