package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicDivisionBriefResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String platformCode;

    private String divisionName;

    private String divisionNameEn;

    private String divisionStatus;

    private Integer maxTeamCount;

    private Integer lineupMinCount;

    private Integer lineupMaxCount;
}
