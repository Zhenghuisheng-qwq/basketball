package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerProfileVo {
    private Long playerId;
    private Long memberId;
    private String nickname;
    private Integer gender;
    private Integer profileStatus;
    private String playerNo;
    private String gameTerminal;
    private String gameAccount;
    private String gameNickname;
    private String positionCode;
    private Integer certificationStatus;
    private String certificationStatusName;
    private Integer playerStatus;
    private Date certifiedTime;
    private Long currentTeamId;
    private String currentTeamName;
}
