package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4PublicDivisionListItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String divisionName;

    private String divisionNameEn;

    private String platformCode;

    private String gameMode;

    private String registrationMode;

    private String divisionStatus;

    private Date registrationStartTime;

    private Date registrationEndTime;

    private String snapshotStatus;
}
