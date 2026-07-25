package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketBusinessAudit;
import com.zhs.basket.query.BusinessAuditQuery;
import com.zhs.basket.vo.BusinessAuditDetailVo;
import com.zhs.basket.vo.BusinessAuditFieldVo;
import com.zhs.basket.vo.BusinessAuditListVo;

import java.util.List;

public interface BasketBusinessAuditMapper {
    int insert(BasketBusinessAudit row);

    List<BusinessAuditListVo> selectList(BusinessAuditQuery query);

    BusinessAuditDetailVo selectById(Long id);

    List<BusinessAuditFieldVo> selectFieldsByAuditId(Long auditId);
}
