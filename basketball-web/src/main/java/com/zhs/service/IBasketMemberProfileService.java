package com.zhs.service;


import com.zhs.model.BasketMemberProfile;
import com.zhs.dto.BasketMemberProfileDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMemberProfileService extends IService<BasketMemberProfile> {
    R addOrUpdate(BasketMemberProfileDto basketMemberProfileDto);
}
