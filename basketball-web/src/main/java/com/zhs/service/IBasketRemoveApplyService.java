package com.zhs.service;


import com.zhs.model.BasketRemoveApply;
import com.zhs.dto.BasketRemoveApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketRemoveApplyService extends IService<BasketRemoveApply> {
    R addOrUpdate(BasketRemoveApplyDto basketRemoveApplyDto);
}
