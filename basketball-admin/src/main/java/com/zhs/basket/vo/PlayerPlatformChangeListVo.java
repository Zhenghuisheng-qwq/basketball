package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class PlayerPlatformChangeListVo {
    private Long id;
    private String requestNo;
    private Long playerId;
    private Long memberId;
    private String nickname;
    private String email;
    private String playerNo;
    private String changeType;
    private String changeTypeName;
    private String platform;
    private String oldDisplayGameId;
    private String newDisplayGameId;
    private Integer requestStatus;
    private String requestStatusName;
    private Date submitTime;
    private Date reviewedTime;
}
