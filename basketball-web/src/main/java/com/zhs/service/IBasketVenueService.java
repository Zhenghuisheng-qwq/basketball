package com.zhs.service;


import com.zhs.model.BasketVenue;
import com.zhs.dto.BasketVenueDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketVenueService extends IService<BasketVenue> {
    R addOrUpdate(BasketVenueDto basketVenueDto);
}
