package com.zhs.service;


import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.dto.BasketAccountSecurityLogDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketAccountSecurityLogService extends IService<BasketAccountSecurityLog> {
    R addOrUpdate(BasketAccountSecurityLogDto basketAccountSecurityLogDto);
}
