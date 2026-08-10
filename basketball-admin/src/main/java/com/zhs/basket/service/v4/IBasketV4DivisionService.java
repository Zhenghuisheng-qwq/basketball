package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketDivisionCreateReq;
import com.zhs.basket.dto.v4.BasketDivisionUpdateReq;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.vo.v4.BasketDivisionListVo;

import java.util.List;

/**
 * CNPA 赛事域 V4：Division 平台赛区管理。
 */
public interface IBasketV4DivisionService {

    List<BasketDivisionListVo> selectList(BasketDivisionQuery query);

    BasketDivisionListVo selectById(Long id);

    Long create(BasketDivisionCreateReq req);

    void update(Long id, BasketDivisionUpdateReq req);

    void openRegistration(Long id);

    void closeRegistration(Long id);

    /** 结束赛事并触发球队生命周期收尾（幂等） */
    void finish(Long id, String reason);

    void delete(Long id);
}
