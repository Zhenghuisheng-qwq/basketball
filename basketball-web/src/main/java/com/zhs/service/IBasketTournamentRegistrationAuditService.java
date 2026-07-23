package com.zhs.service;


import com.zhs.model.BasketTournamentRegistrationAudit;
import com.zhs.dto.BasketTournamentRegistrationAuditDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentRegistrationAuditService extends IService<BasketTournamentRegistrationAudit> {
    R addOrUpdate(BasketTournamentRegistrationAuditDto basketTournamentRegistrationAuditDto);
}
