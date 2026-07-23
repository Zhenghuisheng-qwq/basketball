package com.zhs.service;


import com.zhs.model.BasketStageStanding;
import com.zhs.dto.BasketStageStandingDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketStageStandingService extends IService<BasketStageStanding> {
    R addOrUpdate(BasketStageStandingDto basketStageStandingDto);
}
