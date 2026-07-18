package com.zhs.service;


import com.zhs.model.BasketLeaveApply;
import com.zhs.dto.BasketLeaveApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketLeaveApplyService extends IService<BasketLeaveApply> {
    R addOrUpdate(BasketLeaveApplyDto basketLeaveApplyDto);
}
