package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4PublicDivisionDetailResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String seriesCode;

    private String seriesName;

    private String seasonTitle;

    private String seasonName;

    private String platformCode;

    private String divisionName;

    private String registrationMode;

    private String formatType;

    private Integer maxTeamCount;

    private Integer lineupMinCount;

    private Integer lineupMaxCount;

    private Date registrationStartTime;

    private Date registrationEndTime;

    private Date matchStartTime;

    private Date matchEndTime;

    private String divisionStatus;

    private Integer eventTeamCount;

    private Integer matchCount;

    private Integer finishedMatchCount;
}
