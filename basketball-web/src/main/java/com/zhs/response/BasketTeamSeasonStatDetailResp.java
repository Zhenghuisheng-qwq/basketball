package com.zhs.response;

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
 * @since 2026-07-24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketTeamSeasonStatDetailResp")
public class BasketTeamSeasonStatDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事阶段主键")
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @ApiModelProperty(value = "比赛场次", required = true)
        private Integer games;

        @ApiModelProperty(value = "胜场", required = true)
        private Integer wins;

        @ApiModelProperty(value = "负场", required = true)
        private Integer losses;

        @ApiModelProperty(value = "总得分", required = true)
        private Integer pointsFor;

        @ApiModelProperty(value = "总失分", required = true)
        private Integer pointsAgainst;

        @ApiModelProperty(value = "场均得分", required = true)
        private java.math.BigDecimal avgPoints;

        @ApiModelProperty(value = "场均失分", required = true)
        private java.math.BigDecimal avgPointsAgainst;

        @ApiModelProperty(value = "场均篮板", required = true)
        private java.math.BigDecimal avgRebounds;

        @ApiModelProperty(value = "场均助攻", required = true)
        private java.math.BigDecimal avgAssists;

        @ApiModelProperty(value = "场均抢断", required = true)
        private java.math.BigDecimal avgSteals;

        @ApiModelProperty(value = "场均盖帽", required = true)
        private java.math.BigDecimal avgBlocks;

        @ApiModelProperty(value = "场均失误", required = true)
        private java.math.BigDecimal avgTurnovers;

        @ApiModelProperty(value = "投篮命中率", required = true)
        private java.math.BigDecimal fieldGoalPct;

        @ApiModelProperty(value = "三分命中率", required = true)
        private java.math.BigDecimal threePointPct;

        @ApiModelProperty(value = "罚球命中率", required = true)
        private java.math.BigDecimal freeThrowPct;

        @ApiModelProperty(value = "胜率", required = true)
        private java.math.BigDecimal winRate;

        @ApiModelProperty(value = "最后计算时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date lastCalculatedTime;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "删除标记", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
