package com.zhs.basket.service;

import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.vo.SeasonDetailVo;
import com.zhs.basket.vo.SeasonListVo;
import java.util.List;

public interface IBasketSeasonService {
    List<SeasonListVo> selectByTournamentId(Long tournamentId);
    SeasonDetailVo selectById(Long seasonId);
    Long create(Long tournamentId, SeasonCreateReq req);
    void update(Long seasonId, SeasonUpdateReq req);
    void publish(Long seasonId);
    void openRegistration(Long seasonId);
    void closeRegistration(Long seasonId);
    void start(Long seasonId);
    void finish(Long seasonId);
    void delete(Long seasonId);
}
