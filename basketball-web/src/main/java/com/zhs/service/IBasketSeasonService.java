package com.zhs.service;


import com.zhs.model.BasketSeason;
import com.zhs.dto.BasketSeasonDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketSeasonService extends IService<BasketSeason> {
    R addOrUpdate(BasketSeasonDto basketSeasonDto);
}
