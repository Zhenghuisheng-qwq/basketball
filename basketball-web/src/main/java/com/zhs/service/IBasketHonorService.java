package com.zhs.service;


import com.zhs.model.BasketHonor;
import com.zhs.dto.BasketHonorDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketHonorService extends IService<BasketHonor> {
    R addOrUpdate(BasketHonorDto basketHonorDto);
}
