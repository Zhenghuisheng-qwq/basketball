package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerListVo {
    private Long memberId;
    private String accountNo;
    private String email;
    private String nickname;
    private Long playerId;
    private String playerNo;
    private String gameTerminal;
    private String gameAccount;
    private Integer certificationStatus;
    private String certificationStatusName;
    private Integer playerStatus;
    private Integer accountStatus;
    private Date certifiedTime;
    private Date createTime;
}
