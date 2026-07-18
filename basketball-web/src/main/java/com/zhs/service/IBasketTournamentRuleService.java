package com.zhs.service;


import com.zhs.model.BasketTournamentRule;
import com.zhs.dto.BasketTournamentRuleDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentRuleService extends IService<BasketTournamentRule> {
    R addOrUpdate(BasketTournamentRuleDto basketTournamentRuleDto);
}
