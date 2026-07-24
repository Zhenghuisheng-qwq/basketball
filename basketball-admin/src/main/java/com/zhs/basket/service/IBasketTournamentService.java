package com.zhs.basket.service;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import java.util.List;

public interface IBasketTournamentService {
    List<TournamentListVo> selectList(TournamentQuery query);
    TournamentDetailVo selectById(Long id);
    Long create(TournamentCreateReq req);
    void update(Long id, TournamentUpdateReq req);
    void publish(Long id);
    void unpublish(Long id);
    void close(Long id);
    void delete(Long id);
}
