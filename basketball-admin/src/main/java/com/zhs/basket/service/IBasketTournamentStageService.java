package com.zhs.basket.service;

import com.zhs.basket.query.StageQuery;
import com.zhs.basket.req.StageCreateReq;
import com.zhs.basket.req.StageUpdateReq;
import com.zhs.basket.vo.StageDetailVo;
import com.zhs.basket.vo.StageListVo;
import java.util.List;

public interface IBasketTournamentStageService {
    List<StageListVo> selectList(StageQuery query);
    StageDetailVo selectById(Long stageId);
    Long create(StageCreateReq req);
    void update(Long stageId, StageUpdateReq req);
    void activate(Long stageId);
    void finish(Long stageId);
    void cancel(Long stageId);
    void delete(Long stageId);
}
