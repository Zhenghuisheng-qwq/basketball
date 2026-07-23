package com.zhs.service;


import com.zhs.model.BasketFavorite;
import com.zhs.dto.BasketFavoriteDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketFavoriteService extends IService<BasketFavorite> {
    R addOrUpdate(BasketFavoriteDto basketFavoriteDto);
}
