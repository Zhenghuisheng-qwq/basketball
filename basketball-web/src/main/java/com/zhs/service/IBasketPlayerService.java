package com.zhs.service;


import com.zhs.model.BasketPlayer;
import com.zhs.dto.BasketPlayerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketPlayerService extends IService<BasketPlayer> {
    R addOrUpdate(BasketPlayerDto basketPlayerDto);
}
