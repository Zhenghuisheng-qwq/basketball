package com.zhs.service;


import com.zhs.model.BasketWebUserRole;
import com.zhs.dto.BasketWebUserRoleDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebUserRoleService extends IService<BasketWebUserRole> {
    R addOrUpdate(BasketWebUserRoleDto basketWebUserRoleDto);
}
