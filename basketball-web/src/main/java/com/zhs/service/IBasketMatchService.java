package com.zhs.service;


import com.zhs.model.BasketMatch;
import com.zhs.dto.BasketMatchDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketMatchService extends IService<BasketMatch> {
    R addOrUpdate(BasketMatchDto basketMatchDto);
}
