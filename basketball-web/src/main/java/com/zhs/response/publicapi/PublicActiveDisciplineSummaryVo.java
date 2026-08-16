package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("公开有效处罚摘要")
public class PublicActiveDisciplineSummaryVo implements Serializable {

    @ApiModelProperty("处罚对象 ACCOUNT/PLAYER/PERMANENT_TEAM/EVENT_TEAM")
    private String targetType;

    @ApiModelProperty("处罚措施编码")
    private String penaltyCode;

    @ApiModelProperty("处罚类型公开文案")
    private String penaltyLabel;

    @ApiModelProperty("Hero 状态标签")
    private String tagLabel;

    @ApiModelProperty("公开说明，不含内部备注")
    private String description;

    @ApiModelProperty("业务状态，仅 ACTIVE")
    private String status;

    @ApiModelProperty("状态公开文案")
    private String statusLabel;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveStartTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String divisionName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String seasonName;

    private String seriesName;

    @ApiModelProperty("允许公开时才返回原因，默认不返回内部备注")
    private String publicReason;
}
