package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TeamAuditListVo {
    private Long id;
    private String teamNo;
    private String teamName;
    private Integer auditStatus;
    private String auditStatusName;
    private Integer currentMemberCount;
    private Date createTime;
}
