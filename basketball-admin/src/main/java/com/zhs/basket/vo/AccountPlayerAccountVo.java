package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerAccountVo {
    private Long memberId;
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
