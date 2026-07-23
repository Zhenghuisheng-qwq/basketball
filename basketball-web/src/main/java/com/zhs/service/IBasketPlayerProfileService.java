package com.zhs.service;


import com.zhs.model.BasketPlayerProfile;
import com.zhs.dto.BasketPlayerProfileDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerProfileService extends IService<BasketPlayerProfile> {
    R addOrUpdate(BasketPlayerProfileDto basketPlayerProfileDto);
}
