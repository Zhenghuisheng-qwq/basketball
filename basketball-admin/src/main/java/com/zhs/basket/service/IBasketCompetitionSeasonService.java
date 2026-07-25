package com.zhs.basket.service;

import com.zhs.basket.query.CompetitionSeasonQuery;
import com.zhs.basket.req.CompetitionSeasonCreateReq;
import com.zhs.basket.req.CompetitionSeasonUpdateReq;
import com.zhs.basket.vo.CompetitionSeasonDetailVo;
import com.zhs.basket.vo.CompetitionSeasonListVo;

import java.util.List;

public interface IBasketCompetitionSeasonService {
    List<CompetitionSeasonListVo> selectList(CompetitionSeasonQuery query);

    CompetitionSeasonDetailVo selectById(Long id);

    Long create(CompetitionSeasonCreateReq req);

    void update(Long id, CompetitionSeasonUpdateReq req);

    void delete(Long id);

    void requireExists(Long id);
}
