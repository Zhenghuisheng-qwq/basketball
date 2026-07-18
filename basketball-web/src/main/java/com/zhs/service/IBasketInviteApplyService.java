package com.zhs.service;


import com.zhs.model.BasketInviteApply;
import com.zhs.dto.BasketInviteApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketInviteApplyService extends IService<BasketInviteApply> {
    R addOrUpdate(BasketInviteApplyDto basketInviteApplyDto);
}
