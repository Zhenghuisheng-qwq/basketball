package com.zhs.service;


import com.zhs.model.BasketTournamentAwardConfig;
import com.zhs.dto.BasketTournamentAwardConfigDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentAwardConfigService extends IService<BasketTournamentAwardConfig> {
    R addOrUpdate(BasketTournamentAwardConfigDto basketTournamentAwardConfigDto);
}
