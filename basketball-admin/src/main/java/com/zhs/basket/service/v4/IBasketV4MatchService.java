package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketV4MatchUpdateReq;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.vo.v4.BasketV4MatchListVo;

import java.util.List;

public interface IBasketV4MatchService {

    List<BasketV4MatchListVo> selectList(BasketV4MatchQuery query);

    BasketV4MatchListVo selectById(Long id);

    void updateDraft(Long id, BasketV4MatchUpdateReq req);

    void deleteDraft(Long id);
}
