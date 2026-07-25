package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerDetailVo {
    private Long memberId;
    private String accountNo;
    private String username;
    private String email;
    private Integer accountStatus;
    private Date accountCreateTime;
    private Date accountUpdateTime;
    private String nickname;
    private Integer gender;
    private Integer profileStatus;
    private Long playerId;
    private String playerNo;
    private String gameTerminal;
    private String gameAccount;
    private String gameNickname;
    private String positionCode;
    private Integer certificationStatus;
    private Integer playerStatus;
    private Date certifiedTime;
    private Long currentTeamId;
}
