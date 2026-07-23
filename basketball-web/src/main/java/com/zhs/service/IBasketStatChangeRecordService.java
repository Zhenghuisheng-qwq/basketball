package com.zhs.service;


import com.zhs.model.BasketStatChangeRecord;
import com.zhs.dto.BasketStatChangeRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketStatChangeRecordService extends IService<BasketStatChangeRecord> {
    R addOrUpdate(BasketStatChangeRecordDto basketStatChangeRecordDto);
}
