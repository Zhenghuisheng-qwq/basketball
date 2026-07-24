package com.zhs.basket.vo;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class MatchAuditLogVo {
    private List<MatchStatusLogVo> statusLogs;
    private List<StatChangeRecordVo> statChanges;
}
