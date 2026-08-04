package com.zhs.basket.service;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import java.util.List;

public interface IBasketTournamentService {
    List<TournamentListVo> selectList(TournamentQuery query);

    TournamentDetailVo selectById(Long id);
}