package com.zhs.basket.support;

import com.zhs.basket.domain.BasketBusinessAudit;
import com.zhs.basket.domain.BasketBusinessAuditField;
import com.zhs.basket.mapper.BasketBusinessAuditFieldMapper;
import com.zhs.basket.mapper.BasketBusinessAuditMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class BasketBusinessAuditWriter {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketBusinessAuditMapper auditMapper;
    @Autowired private BasketBusinessAuditFieldMapper auditFieldMapper;

    public Long writeAudit(String module, String objectType, Long objectId, String actionType, String summary,
            String reason, List<AuditFieldInput> fields) {
        Date now = new Date();
        BasketBusinessAudit audit = new BasketBusinessAudit();
        audit.setAuditNo("AUD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        audit.setBizModule(module);
        audit.setObjectType(objectType);
        audit.setObjectId(objectId);
        audit.setActionType(actionType);
        audit.setActionResult("SUCCESS");
        audit.setChangeSummary(summary);
        audit.setActionReason(reason);
        audit.setOperatorId(SecurityUtils.getUserId());
        audit.setOperatorName(SecurityUtils.getUsername());
        if ("DISCIPLINE".equals(module) && "DISCIPLINE_RECORD".equals(objectType)) {
            audit.setRelatedDisciplineId(objectId);
        }
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        auditMapper.insert(audit);

        if (fields != null && !fields.isEmpty()) {
            List<BasketBusinessAuditField> rows = new ArrayList<>();
            for (AuditFieldInput input : fields) {
                if (input == null || StringUtils.isEmpty(input.getFieldName())) {
                    continue;
                }
                BasketBusinessAuditField row = new BasketBusinessAuditField();
                row.setAuditId(audit.getId());
                row.setFieldName(input.getFieldName());
                row.setFieldLabel(input.getFieldLabel());
                row.setBeforeValue(input.getBeforeValue());
                row.setAfterValue(input.getAfterValue());
                row.setDataType(StringUtils.isNotEmpty(input.getDataType()) ? input.getDataType() : "STRING");
                row.setSensitiveFlag(input.getSensitiveFlag() != null ? input.getSensitiveFlag() : 0);
                row.setCreateTime(now);
                row.setUpdateTime(now);
                row.setIsDelete(NOT_DELETED);
                rows.add(row);
            }
            if (!rows.isEmpty()) {
                auditFieldMapper.batchInsert(rows);
            }
        }
        return audit.getId();
    }
}
