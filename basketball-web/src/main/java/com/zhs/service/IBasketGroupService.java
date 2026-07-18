package com.zhs.service;


import com.zhs.model.BasketGroup;
import com.zhs.dto.BasketGroupDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketGroupService extends IService<BasketGroup> {
    R addOrUpdate(BasketGroupDto basketGroupDto);
}
