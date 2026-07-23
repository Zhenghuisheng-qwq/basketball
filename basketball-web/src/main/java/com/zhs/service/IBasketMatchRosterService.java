package com.zhs.service;


import com.zhs.model.BasketMatchRoster;
import com.zhs.dto.BasketMatchRosterDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMatchRosterService extends IService<BasketMatchRoster> {
    R addOrUpdate(BasketMatchRosterDto basketMatchRosterDto);
}
