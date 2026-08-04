package com.zhs.response.v4;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PersonalFormationAvailableTeamResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;

    private String teamName;

    private String logoUrl;

    private Integer teamStatus;

    private Integer currentMemberCount;

    private Integer matchedAssignedMemberCount;

    private Integer missingAssignedMemberCount;

    private Integer extraMemberCount;

    private Boolean captainMatched;

    private Boolean canBind;

    private String unavailableReason;
}
