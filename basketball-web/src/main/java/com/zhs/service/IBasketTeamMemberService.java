package com.zhs.service;


import com.zhs.model.BasketTeamMember;
import com.zhs.dto.BasketTeamMemberDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTeamMemberService extends IService<BasketTeamMember> {
    R addOrUpdate(BasketTeamMemberDto basketTeamMemberDto);
}
