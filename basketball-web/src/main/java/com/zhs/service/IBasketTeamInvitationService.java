package com.zhs.service;


import com.zhs.model.BasketTeamInvitation;
import com.zhs.dto.BasketTeamInvitationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamInvitationService extends IService<BasketTeamInvitation> {
    R addOrUpdate(BasketTeamInvitationDto basketTeamInvitationDto);
}
