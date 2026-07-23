package com.zhs.service;


import com.zhs.model.BasketDisciplineRecord;
import com.zhs.dto.BasketDisciplineRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketDisciplineRecordService extends IService<BasketDisciplineRecord> {
    R addOrUpdate(BasketDisciplineRecordDto basketDisciplineRecordDto);
}
