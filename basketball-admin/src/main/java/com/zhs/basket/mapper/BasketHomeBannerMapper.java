package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketHomeBanner;
import com.zhs.basket.req.HomeBannerQueryReq;
import com.zhs.basket.resp.HomeBannerListResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketHomeBannerMapper {
    BasketHomeBanner selectById(@Param("id") Long id);

    HomeBannerListResp selectDetailById(@Param("id") Long id);

    List<HomeBannerListResp> selectList(HomeBannerQueryReq query);

    int insert(BasketHomeBanner banner);

    int updateById(BasketHomeBanner banner);

    int updateStatusIfMatch(@Param("id") Long id,
                            @Param("fromStatus") Integer fromStatus,
                            @Param("toStatus") Integer toStatus);

    int softDelete(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses);
}
