package com.zhs.service;


import com.zhs.model.BasketFileAsset;
import com.zhs.dto.BasketFileAssetDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketFileAssetService extends IService<BasketFileAsset> {
    R addOrUpdate(BasketFileAssetDto basketFileAssetDto);
}
