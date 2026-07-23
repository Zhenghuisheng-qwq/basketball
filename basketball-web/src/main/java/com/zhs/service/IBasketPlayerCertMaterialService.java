package com.zhs.service;


import com.zhs.model.BasketPlayerCertMaterial;
import com.zhs.dto.BasketPlayerCertMaterialDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerCertMaterialService extends IService<BasketPlayerCertMaterial> {
    R addOrUpdate(BasketPlayerCertMaterialDto basketPlayerCertMaterialDto);
}
