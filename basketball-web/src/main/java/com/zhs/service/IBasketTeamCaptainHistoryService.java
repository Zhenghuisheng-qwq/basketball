package com.zhs.service;


import com.zhs.model.BasketTeamCaptainHistory;
import com.zhs.dto.BasketTeamCaptainHistoryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamCaptainHistoryService extends IService<BasketTeamCaptainHistory> {
    R addOrUpdate(BasketTeamCaptainHistoryDto basketTeamCaptainHistoryDto);
}
