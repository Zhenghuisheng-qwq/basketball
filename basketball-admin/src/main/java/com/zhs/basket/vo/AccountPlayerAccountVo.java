package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerAccountVo {
    /** 用户ID（历史字段 memberId） */
    private Long memberId;
    private Long userId;
    private String accountNo;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer emailVerified;
    private Integer accountStatus;
    private String accountStatusName;
    private Date lastLoginTime;
    private String lastLoginIp;
    private Date createTime;
    private Date updateTime;
}
