package com.zhs.service;


import com.zhs.model.BasketWebLoginLog;
import com.zhs.dto.BasketWebLoginLogDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketWebLoginLogService extends IService<BasketWebLoginLog> {
    R addOrUpdate(BasketWebLoginLogDto basketWebLoginLogDto);
}
