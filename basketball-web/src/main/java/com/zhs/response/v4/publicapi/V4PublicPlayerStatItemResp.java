package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V4PublicPlayerStatItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    private String playerName;

    private String teamName;

    private Integer matchCount;

    private Integer points;

    private BigDecimal avgPoints;

    private Integer rebound;

    private BigDecimal avgRebound;

    private Integer assist;

    private BigDecimal avgAssist;

    private Integer steal;

    private Integer block;

    private Integer turnover;
}
