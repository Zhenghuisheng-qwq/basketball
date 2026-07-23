package com.zhs.service;


import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.dto.BasketPlayerCertApplicationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerCertApplicationService extends IService<BasketPlayerCertApplication> {
    R addOrUpdate(BasketPlayerCertApplicationDto basketPlayerCertApplicationDto);
}
