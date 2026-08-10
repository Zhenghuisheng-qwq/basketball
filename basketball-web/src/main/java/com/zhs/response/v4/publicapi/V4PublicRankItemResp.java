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

    /** 永久球员 ID，公开 /player/{id} 路由必须使用此字段 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;

    private String teamName;

    private String playerName;

    private BigDecimal rankValue;
}
