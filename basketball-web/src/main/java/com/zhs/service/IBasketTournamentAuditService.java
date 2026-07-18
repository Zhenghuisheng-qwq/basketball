package com.zhs.service;


import com.zhs.model.BasketTournamentAudit;
import com.zhs.dto.BasketTournamentAuditDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentAuditService extends IService<BasketTournamentAudit> {
    R addOrUpdate(BasketTournamentAuditDto basketTournamentAuditDto);
}
