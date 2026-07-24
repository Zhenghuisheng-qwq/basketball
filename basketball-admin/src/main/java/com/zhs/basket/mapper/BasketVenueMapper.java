package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketVenue;
import com.zhs.basket.vo.VenueSimpleVo;

import java.util.List;

public interface BasketVenueMapper {
    BasketVenue selectById(Long id);
    List<VenueSimpleVo> selectSimpleList();
}
