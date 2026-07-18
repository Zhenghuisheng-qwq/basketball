package com.zhs.service;


import com.zhs.model.BasketWebRole;
import com.zhs.dto.BasketWebRoleDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebRoleService extends IService<BasketWebRole> {
    R addOrUpdate(BasketWebRoleDto basketWebRoleDto);
}
