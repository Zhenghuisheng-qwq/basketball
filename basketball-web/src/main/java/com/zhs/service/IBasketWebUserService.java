package com.zhs.service;


import com.zhs.model.BasketWebUser;
import com.zhs.dto.BasketWebUserDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebUserService extends IService<BasketWebUser> {
    R addOrUpdate(BasketWebUserDto basketWebUserDto);
}
