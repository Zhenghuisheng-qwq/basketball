package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketLeaderboardPolicy {
    private Long id;
    private Long divisionId;
    private String scopeKey;
    private Long stageId;
    private Integer versionNo;
    private String lifecycle;
    private String templateCode;
    private String rankingAudience;
    private String configJson;
    private Long basedOnPolicyId;
    private Integer expectedLock;
    private String draftScopeKey;
    private String activeScopeKey;
    private String publishNote;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private Integer isDelete;
}
