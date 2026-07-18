package com.zhs.service;


import com.zhs.model.BasketFileResource;
import com.zhs.dto.BasketFileResourceDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketFileResourceService extends IService<BasketFileResource> {
    R addOrUpdate(BasketFileResourceDto basketFileResourceDto);
}
