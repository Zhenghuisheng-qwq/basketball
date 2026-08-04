package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicStageItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private String stageTypeName;
    private String competitionFormat;
    private String competitionFormatName;
    private String executionStatus;
    private String executionStatusName;
    private Boolean currentStage;
}
