package com.zhs.service;


import com.zhs.model.BasketPartner;
import com.zhs.dto.BasketPartnerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPartnerService extends IService<BasketPartner> {
    R addOrUpdate(BasketPartnerDto basketPartnerDto);
}
