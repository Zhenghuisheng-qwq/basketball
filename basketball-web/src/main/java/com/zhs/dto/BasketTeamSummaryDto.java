package com.zhs.dto;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.alibaba.fastjson2.JSONObject;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketTeamSummaryDto")
public class BasketTeamSummaryDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛季编号")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "来源球队编号")
        private Long sourceTeamId;

        @ApiModelProperty(value = "球队名称", required = true)
        private String teamName;

        @ApiModelProperty(value = "比赛场次", required = true)
        private Integer matchCount;

        @ApiModelProperty(value = "胜场", required = true)
        private Integer winCount;

        @ApiModelProperty(value = "负场", required = true)
        private Integer loseCount;

        @ApiModelProperty(value = "平场", required = true)
        private Integer drawCount;

        @ApiModelProperty(value = "总得分", required = true)
        private Integer scoreFor;

        @ApiModelProperty(value = "总失分", required = true)
        private Integer scoreAgainst;

        @ApiModelProperty(value = "净胜分", required = true)
        private Integer scoreDiff;

        @ApiModelProperty(value = "积分", required = true)
        private Integer points;

        @ApiModelProperty(value = "汇总类型", required = true)
        private String summaryType;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新者")
        private String updateBy;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
