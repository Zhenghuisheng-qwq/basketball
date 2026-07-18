package com.zhs.service;


import com.zhs.model.BasketEmailCode;
import com.zhs.dto.BasketEmailCodeDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketEmailCodeService extends IService<BasketEmailCode> {
    R addOrUpdate(BasketEmailCodeDto basketEmailCodeDto);
}
