package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketFileAsset;

public interface BasketFileAssetMapper {
    BasketFileAsset selectById(Long id);

    int insert(BasketFileAsset asset);
}
