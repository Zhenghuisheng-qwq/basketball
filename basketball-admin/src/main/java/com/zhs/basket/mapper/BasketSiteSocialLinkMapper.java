package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketSiteSocialLink;
import com.zhs.basket.resp.SiteSocialLinkListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketSiteSocialLinkMapper {
    BasketSiteSocialLink selectById(@Param("id") Long id);

    List<SiteSocialLinkListResp> selectList();

    int updateById(BasketSiteSocialLink link);
}
