package com.zhs.service;


import com.zhs.model.BasketTransferApply;
import com.zhs.dto.BasketTransferApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTransferApplyService extends IService<BasketTransferApply> {
    R addOrUpdate(BasketTransferApplyDto basketTransferApplyDto);
}
