package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicStandingItemResp {

    private Integer rankNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    private String teamName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamLogoFileId;

    private String teamLogoUrl;

    private Integer winCount;

    private Integer loseCount;

    private Integer pointFor;

    private Integer pointAgainst;

    private Integer pointDiff;

    private Integer standingPoints;

    private Long stageId;

    private Long groupId;

    private String qualificationStatus;
}
