package com.zhs.service;


import com.zhs.model.BasketMessageReceiver;
import com.zhs.dto.BasketMessageReceiverDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMessageReceiverService extends IService<BasketMessageReceiver> {
    R addOrUpdate(BasketMessageReceiverDto basketMessageReceiverDto);
}
