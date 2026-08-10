package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MemberProfileChangeListVo {
    private Long id;
    private String changeNo;
    private Long memberId;
    private String accountNo;
    private Long playerId;
    private String playerNo;
    private String submitUserName;
    private String submitUserEmail;
    private String oldNickname;
    private String newNickname;
    private String oldAvatarUrl;
    private String newAvatarUrl;
    private String changeSummary;
    private Integer auditStatus;
    private String auditStatusName;
    private Date submitTime;
    private Date auditTime;
}
