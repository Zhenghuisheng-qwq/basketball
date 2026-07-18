package com.zhs.service;


import com.zhs.model.BasketRound;
import com.zhs.dto.BasketRoundDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketRoundService extends IService<BasketRound> {
    R addOrUpdate(BasketRoundDto basketRoundDto);
}
