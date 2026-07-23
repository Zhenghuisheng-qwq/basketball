package com.zhs.service;


import com.zhs.model.BasketTeamAuditRecord;
import com.zhs.dto.BasketTeamAuditRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamAuditRecordService extends IService<BasketTeamAuditRecord> {
    R addOrUpdate(BasketTeamAuditRecordDto basketTeamAuditRecordDto);
}
