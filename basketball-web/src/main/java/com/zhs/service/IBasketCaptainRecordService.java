package com.zhs.service;


import com.zhs.model.BasketCaptainRecord;
import com.zhs.dto.BasketCaptainRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketCaptainRecordService extends IService<BasketCaptainRecord> {
    R addOrUpdate(BasketCaptainRecordDto basketCaptainRecordDto);
}
