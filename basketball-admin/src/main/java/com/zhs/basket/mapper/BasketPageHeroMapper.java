package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPageHero;
import com.zhs.basket.resp.PageHeroResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPageHeroMapper {
    PageHeroResp selectDetail(@Param("pageKey") String pageKey, @Param("seasonId") Long seasonId);

    List<PageHeroResp> selectGenericList();

    BasketPageHero selectForUpdate(@Param("pageKey") String pageKey, @Param("seasonId") Long seasonId);

    int insert(BasketPageHero hero);

    int updateDraft(BasketPageHero hero);

    int updateWorking(BasketPageHero hero);

    int applyPublish(BasketPageHero hero);
}
