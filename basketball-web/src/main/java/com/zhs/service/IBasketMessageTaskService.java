package com.zhs.service;


import com.zhs.model.BasketMessageTask;
import com.zhs.dto.BasketMessageTaskDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMessageTaskService extends IService<BasketMessageTask> {
    R addOrUpdate(BasketMessageTaskDto basketMessageTaskDto);
}
