package com.zhs.service;


import com.zhs.model.BasketTournamentGroup;
import com.zhs.dto.BasketTournamentGroupDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentGroupService extends IService<BasketTournamentGroup> {
    R addOrUpdate(BasketTournamentGroupDto basketTournamentGroupDto);
}
