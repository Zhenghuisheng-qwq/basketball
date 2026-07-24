package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开球员详情")
public class PublicPlayerDetailVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    private String playerNo;
    private String displayName;
    private String avatarUrl;
    private String positionCode;
    private String dominantHand;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long currentTeamId;
    private String currentTeamName;
    private Integer certificationStatus;
    private Integer playerStatus;

    @ApiModelProperty("资料是否公开；false 时仅返回最小信息")
    private Boolean profilePublic;

    @ApiModelProperty("不可公开时的提示")
    private String privacyMessage;
}
