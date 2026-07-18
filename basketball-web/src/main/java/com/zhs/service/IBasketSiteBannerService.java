package com.zhs.service;


import com.zhs.model.BasketSiteBanner;
import com.zhs.dto.BasketSiteBannerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketSiteBannerService extends IService<BasketSiteBanner> {
    R addOrUpdate(BasketSiteBannerDto basketSiteBannerDto);
}
