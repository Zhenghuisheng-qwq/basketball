package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerListVo {
    private Long memberId;
    private String accountNo;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer emailVerified;
    private Long playerId;
    private String playerNo;
    private String gameTerminal;
    private String gameAccount;
    private Integer certificationStatus;
    private String certificationStatusName;
    private Integer latestApplicationStatus;
    private String certDisplayStatus;
    private String certDisplayStatusName;
    private Integer playerStatus;
    private Integer accountStatus;
    private String accountStatusName;
    private Long currentTeamId;
    private String currentTeamName;
    private Integer hasCaptainQual;
    private Date lastLoginTime;
    private Date certifiedTime;
    private Date createTime;
}
