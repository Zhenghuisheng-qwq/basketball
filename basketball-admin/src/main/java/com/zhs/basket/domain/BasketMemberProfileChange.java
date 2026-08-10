package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMemberProfileChange {
    private Long id;
    private String changeNo;
    private Long memberId;
    private Long playerId;
    private String oldNickname;
    private String newNickname;
    private Long oldAvatarFileId;
    private Long newAvatarFileId;
    private String oldAvatarUrl;
    private String newAvatarUrl;
    private String changeSummary;
    private Integer auditStatus;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private Integer activeFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
