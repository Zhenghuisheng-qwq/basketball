package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertAudit;
import java.util.List;

public interface BasketPlayerCertAuditMapper {
    int insert(BasketPlayerCertAudit record);
    List<BasketPlayerCertAudit> selectByApplicationId(Long applicationId);
}
