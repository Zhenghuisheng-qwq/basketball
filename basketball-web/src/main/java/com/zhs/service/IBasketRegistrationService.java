package com.zhs.service;


import com.zhs.model.BasketRegistration;
import com.zhs.dto.BasketRegistrationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketRegistrationService extends IService<BasketRegistration> {
    R addOrUpdate(BasketRegistrationDto basketRegistrationDto);
}
