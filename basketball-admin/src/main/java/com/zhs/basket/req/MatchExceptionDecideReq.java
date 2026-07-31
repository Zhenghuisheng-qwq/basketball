package com.zhs.basket.req;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zhs.framework.config.LongStringDeserializer;
import lombok.Data;

@Data
public class MatchExceptionDecideReq {
    private String decisionType;
    private String decisionResult;
    private Integer decisionScoreHome;
    private Integer decisionScoreAway;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LongStringDeserializer.class)
    private Long winnerTeamId;
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LongStringDeserializer.class)
    private Long walkoverTeamId;
    private String scheduledStartTime;
    private String auditRemark;
    private MatchOfficialDataCorrectionReq correctionData;
}
