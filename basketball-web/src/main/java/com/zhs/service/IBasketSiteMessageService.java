package com.zhs.service;


import com.zhs.model.BasketSiteMessage;
import com.zhs.dto.BasketSiteMessageDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketSiteMessageService extends IService<BasketSiteMessage> {
    R addOrUpdate(BasketSiteMessageDto basketSiteMessageDto);
}
