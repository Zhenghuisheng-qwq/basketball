package com.zhs.service;


import com.zhs.model.BasketMatchStatusLog;
import com.zhs.dto.BasketMatchStatusLogDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMatchStatusLogService extends IService<BasketMatchStatusLog> {
    R addOrUpdate(BasketMatchStatusLogDto basketMatchStatusLogDto);
}
