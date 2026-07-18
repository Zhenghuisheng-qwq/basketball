package com.zhs.service;


import com.zhs.model.BasketNotice;
import com.zhs.dto.BasketNoticeDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketNoticeService extends IService<BasketNotice> {
    R addOrUpdate(BasketNoticeDto basketNoticeDto);
}
