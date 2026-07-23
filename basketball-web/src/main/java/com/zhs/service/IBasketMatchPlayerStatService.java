package com.zhs.service;


import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.dto.BasketMatchPlayerStatDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMatchPlayerStatService extends IService<BasketMatchPlayerStat> {
    R addOrUpdate(BasketMatchPlayerStatDto basketMatchPlayerStatDto);
}
