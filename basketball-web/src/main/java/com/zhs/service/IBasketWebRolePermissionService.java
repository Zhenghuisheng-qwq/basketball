package com.zhs.service;


import com.zhs.model.BasketWebRolePermission;
import com.zhs.dto.BasketWebRolePermissionDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebRolePermissionService extends IService<BasketWebRolePermission> {
    R addOrUpdate(BasketWebRolePermissionDto basketWebRolePermissionDto);
}
