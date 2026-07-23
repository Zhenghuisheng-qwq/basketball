package com.zhs.service;


import com.zhs.model.BasketCaptainQualification;
import com.zhs.dto.BasketCaptainQualificationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketCaptainQualificationService extends IService<BasketCaptainQualification> {
    R addOrUpdate(BasketCaptainQualificationDto basketCaptainQualificationDto);
}
