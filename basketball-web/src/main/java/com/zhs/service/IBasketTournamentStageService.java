package com.zhs.service;


import com.zhs.model.BasketTournamentStage;
import com.zhs.dto.BasketTournamentStageDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentStageService extends IService<BasketTournamentStage> {
    R addOrUpdate(BasketTournamentStageDto basketTournamentStageDto);
}
