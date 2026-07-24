package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRule;
import java.util.List;

public interface BasketTournamentRuleMapper {
    List<BasketTournamentRule> selectBySeasonId(Long seasonId);
    BasketTournamentRule selectById(Long id);
    int insert(BasketTournamentRule rule);
    int updateById(BasketTournamentRule rule);
    int softDelete(Long id);
}
