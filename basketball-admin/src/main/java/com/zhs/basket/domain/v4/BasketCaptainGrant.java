package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketCaptainGrant
 */
@Data
public class BasketCaptainGrant {
    private Long id;
    private Long divisionId;
    private Long seasonId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String grantStatus;
    /** ACTIVE 时写入 divisionId_playerId；REVOKED/EXPIRED/软删除为 null */
    private String activeUniqueKey;
    private Date validFrom;
    private Date validTo;
    /** AUTO_EVENT / CUSTOM；历史数据为空，表示尚未确认，不自动改期 */
    private String validToMode;
    private Long grantedBy;
    private String remark;
    private String grantSource;
    private Long sourceDrawTeamId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
