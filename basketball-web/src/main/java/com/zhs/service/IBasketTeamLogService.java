package com.zhs.service;


import com.zhs.model.BasketTeamLog;
import com.zhs.dto.BasketTeamLogDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTeamLogService extends IService<BasketTeamLog> {
    R addOrUpdate(BasketTeamLogDto basketTeamLogDto);
}
