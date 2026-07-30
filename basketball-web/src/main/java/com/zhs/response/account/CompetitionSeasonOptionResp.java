package com.zhs.response.account;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("顶层赛季筛选项")
public class CompetitionSeasonOptionResp implements Serializable {

    @JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    @ApiModelProperty("顶层赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季展示名称，如 2026赛季")
    private String seasonName;

    @ApiModelProperty("赛季年份")
    private Integer seasonYear;
}
