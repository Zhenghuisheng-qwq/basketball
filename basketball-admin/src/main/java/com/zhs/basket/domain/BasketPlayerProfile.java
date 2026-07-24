package com.zhs.basket.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketPlayerProfile {
    private Long id;
    private Long memberId;
    private String playerNo;
    private String realName;
    private String idType;
    private String idNoCipher;
    private String idNoMasked;
    private String gameTerminal;
    private String gameAccount;
    private String gameNickname;
    private String positionCode;
    private String dominantHand;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private Long currentTeamId;
    private Integer certificationStatus;
    private Integer playerStatus;
    private Date certifiedTime;
    private Integer profileVisible;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
