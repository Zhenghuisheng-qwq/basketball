package com.zhs.basket.support;

import com.zhs.basket.enums.ScheduleAuditActionTypeEnum;
import com.zhs.basket.req.AuditFieldInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduleAuditSupport {

    private static final String MODULE = "SCHEDULE";
    private static final String OBJECT_MATCH = "MATCH";
    private static final String OBJECT_BATCH = "SCHEDULE_BATCH";

    @Autowired
    private BasketBusinessAuditWriter auditWriter;

    public void logMatchAction(Long matchId, Long eventId, Long batchId, ScheduleAuditActionTypeEnum action,
            String summary, String reason, List<AuditFieldInput> fields) {
        auditWriter.writeAudit(MODULE, OBJECT_MATCH, matchId, action.getCode(), enrichSummary(eventId, batchId, summary),
                reason, fields);
    }

    public void logBatchAction(Long batchId, Long eventId, ScheduleAuditActionTypeEnum action,
            String summary, String reason, List<AuditFieldInput> fields) {
        auditWriter.writeAudit(MODULE, OBJECT_BATCH, batchId, action.getCode(), enrichSummary(eventId, batchId, summary),
                reason, fields);
    }

    public AuditFieldInput field(String name, String label, Object before, Object after) {
        AuditFieldInput input = new AuditFieldInput();
        input.setFieldName(name);
        input.setFieldLabel(label);
        input.setBeforeValue(before == null ? null : String.valueOf(before));
        input.setAfterValue(after == null ? null : String.valueOf(after));
        return input;
    }

    public List<AuditFieldInput> singleField(String name, String label, Object before, Object after) {
        List<AuditFieldInput> fields = new ArrayList<>(1);
        fields.add(field(name, label, before, after));
        return fields;
    }

    private String enrichSummary(Long eventId, Long batchId, String summary) {
        StringBuilder sb = new StringBuilder(summary == null ? "" : summary);
        if (eventId != null) {
            sb.append(" [eventId=").append(eventId).append("]");
        }
        if (batchId != null) {
            sb.append(" [batchId=").append(batchId).append("]");
        }
        return sb.toString();
    }
}
