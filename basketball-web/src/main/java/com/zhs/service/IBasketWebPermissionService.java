package com.zhs.service;


import com.zhs.model.BasketWebPermission;
import com.zhs.dto.BasketWebPermissionDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebPermissionService extends IService<BasketWebPermission> {
    R addOrUpdate(BasketWebPermissionDto basketWebPermissionDto);
}
