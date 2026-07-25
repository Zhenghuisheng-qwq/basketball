package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TeamAuditListVo {
    private Long id;
    private String teamNo;
    private String teamName;
    private String shortName;
    private Long logoFileId;
    private String logoUrl;
    private String gameTerminal;
    private Integer publicStatus;
    private String publicStatusName;
    private Integer auditStatus;
    private String auditStatusName;
    private Integer currentMemberCount;
    private Date createTime;
}
