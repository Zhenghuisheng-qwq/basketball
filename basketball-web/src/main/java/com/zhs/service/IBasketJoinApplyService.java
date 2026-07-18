package com.zhs.service;


import com.zhs.model.BasketJoinApply;
import com.zhs.dto.BasketJoinApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketJoinApplyService extends IService<BasketJoinApply> {
    R addOrUpdate(BasketJoinApplyDto basketJoinApplyDto);
}
