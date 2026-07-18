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
@ApiModel(value = "BasketPlayerSummaryDto")
public class BasketPlayerSummaryDto implements Serializable {
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
        @ApiModelProperty(value = "来源球员编号")
        private Long sourcePlayerId;

        @ApiModelProperty(value = "球员名称", required = true)
        private String playerName;

        @ApiModelProperty(value = "比赛场次", required = true)
        private Integer matchCount;

        @ApiModelProperty(value = "总得分", required = true)
        private Integer pointsTotal;

        @ApiModelProperty(value = "总篮板", required = true)
        private Integer reboundsTotal;

        @ApiModelProperty(value = "总助攻", required = true)
        private Integer assistsTotal;

        @ApiModelProperty(value = "总抢断", required = true)
        private Integer stealsTotal;

        @ApiModelProperty(value = "总盖帽", required = true)
        private Integer blocksTotal;

        @ApiModelProperty(value = "场均得分", required = true)
        private java.math.BigDecimal pointsAvg;

        @ApiModelProperty(value = "场均篮板", required = true)
        private java.math.BigDecimal reboundsAvg;

        @ApiModelProperty(value = "场均助攻", required = true)
        private java.math.BigDecimal assistsAvg;

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
