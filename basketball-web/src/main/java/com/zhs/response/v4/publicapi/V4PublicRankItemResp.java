package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class V4PublicRankItemResp {

    private Integer rankNo;

    private String rankType;

    private String targetType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    private String teamName;

    private String playerName;

    private BigDecimal rankValue;
}
