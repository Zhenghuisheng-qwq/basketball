package com.zhs.service;


import com.zhs.model.BasketPlayerSeasonStat;
import com.zhs.dto.BasketPlayerSeasonStatDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerSeasonStatService extends IService<BasketPlayerSeasonStat> {
    R addOrUpdate(BasketPlayerSeasonStatDto basketPlayerSeasonStatDto);
}
