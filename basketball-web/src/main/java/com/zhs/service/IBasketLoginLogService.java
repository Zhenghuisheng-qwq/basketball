package com.zhs.service;


import com.zhs.model.BasketLoginLog;
import com.zhs.dto.BasketLoginLogDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketLoginLogService extends IService<BasketLoginLog> {
    R addOrUpdate(BasketLoginLogDto basketLoginLogDto);
}
