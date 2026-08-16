package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicEventTeamItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceTeamId;

    private String teamName;

    private String teamShortName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamLogoFileId;

    private String teamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainPlayerId;

    private String captainName;

    private String platformCode;

    private String eventTeamStatus;

    private String eventTeamStatusName;

    private Boolean disqualified;

    private Boolean temporaryTeam;
}
