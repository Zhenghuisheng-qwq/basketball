package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchTeam {
    private Long id;
    private Long matchId;
    private Long teamId;
    private String sideType;
    private Integer score;
    private String resultType;
    private Integer seedNo;
    private Integer timeoutCount;
    private Integer foulsTotal;
    private Integer isWinner;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
