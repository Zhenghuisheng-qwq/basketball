package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicEventPlayerItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;

    private String playerName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long avatarFileId;

    private String avatarUrl;

    private String gameIdSnapshot;

    private String platformCode;

    private String position;

    private String jerseyNo;

    private Integer isCaptain;

    private Integer isStarter;
}
