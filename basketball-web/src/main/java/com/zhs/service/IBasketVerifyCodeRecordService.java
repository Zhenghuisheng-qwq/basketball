package com.zhs.service;


import com.zhs.model.BasketVerifyCodeRecord;
import com.zhs.dto.BasketVerifyCodeRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketVerifyCodeRecordService extends IService<BasketVerifyCodeRecord> {
    R addOrUpdate(BasketVerifyCodeRecordDto basketVerifyCodeRecordDto);
}
