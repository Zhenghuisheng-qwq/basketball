package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketCaptainGrantListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Long teamId;
    private String teamName;
    private String teamBoundLabel;
    private Long playerId;
    private String playerName;
    private Long memberId;
    private String memberNickname;
    private String grantStatus;
    private String grantSource;
    private Date validFrom;
    private Date validTo;
    private String remark;
    private Date createTime;
}
