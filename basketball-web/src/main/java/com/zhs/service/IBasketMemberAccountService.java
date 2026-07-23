package com.zhs.service;


import com.zhs.model.BasketMemberAccount;
import com.zhs.dto.BasketMemberAccountDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMemberAccountService extends IService<BasketMemberAccount> {
    R addOrUpdate(BasketMemberAccountDto basketMemberAccountDto);
}
