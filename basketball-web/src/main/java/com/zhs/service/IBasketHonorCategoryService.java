package com.zhs.service;


import com.zhs.model.BasketHonorCategory;
import com.zhs.dto.BasketHonorCategoryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketHonorCategoryService extends IService<BasketHonorCategory> {
    R addOrUpdate(BasketHonorCategoryDto basketHonorCategoryDto);
}
