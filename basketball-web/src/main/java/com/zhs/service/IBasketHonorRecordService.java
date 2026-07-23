package com.zhs.service;


import com.zhs.model.BasketHonorRecord;
import com.zhs.dto.BasketHonorRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketHonorRecordService extends IService<BasketHonorRecord> {
    R addOrUpdate(BasketHonorRecordDto basketHonorRecordDto);
}
