package com.zhs.service;


import com.zhs.model.BasketSiteConfig;
import com.zhs.dto.BasketSiteConfigDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketSiteConfigService extends IService<BasketSiteConfig> {
    R addOrUpdate(BasketSiteConfigDto basketSiteConfigDto);
}
