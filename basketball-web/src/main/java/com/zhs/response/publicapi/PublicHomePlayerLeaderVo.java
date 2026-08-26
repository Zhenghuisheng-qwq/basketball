package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PublicHomePlayerLeaderVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;

    private String playerName;

    private String avatarUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    private String teamLogoUrl;

    private Integer matchCount;

    private BigDecimal value;
}
