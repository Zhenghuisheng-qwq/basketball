package com.zhs.service;


import com.zhs.model.BasketHomeBanner;
import com.zhs.dto.BasketHomeBannerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketHomeBannerService extends IService<BasketHomeBanner> {
    R addOrUpdate(BasketHomeBannerDto basketHomeBannerDto);
}
