package com.zhs.service;


import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.dto.BasketTeamSeasonProfileDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamSeasonProfileService extends IService<BasketTeamSeasonProfile> {
    R addOrUpdate(BasketTeamSeasonProfileDto basketTeamSeasonProfileDto);
}
