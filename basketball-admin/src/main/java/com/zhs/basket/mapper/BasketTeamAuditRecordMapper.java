package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamAuditRecord;
import java.util.List;

public interface BasketTeamAuditRecordMapper {
    int insert(BasketTeamAuditRecord record);
    List<BasketTeamAuditRecord> selectByTeamId(Long teamId);
}
