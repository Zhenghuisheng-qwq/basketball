package com.zhs.service;


import com.zhs.model.BasketRegistrationPlayer;
import com.zhs.dto.BasketRegistrationPlayerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketRegistrationPlayerService extends IService<BasketRegistrationPlayer> {
    R addOrUpdate(BasketRegistrationPlayerDto basketRegistrationPlayerDto);
}
