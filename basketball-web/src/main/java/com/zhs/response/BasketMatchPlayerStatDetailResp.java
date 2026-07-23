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
@ApiModel(value = "BasketMatchPlayerStatDetailResp")
public class BasketMatchPlayerStatDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "比赛主键")
        private Long matchId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事阶段主键")
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员主键")
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "出场名单主键")
        private Long rosterId;

        @ApiModelProperty(value = "首发标记", required = true)
        private Integer startedFlag;

        @ApiModelProperty(value = "出场秒数", required = true)
        private Integer minutesSeconds;

        @ApiModelProperty(value = "得分", required = true)
        private Integer points;

        @ApiModelProperty(value = "篮板", required = true)
        private Integer rebounds;

        @ApiModelProperty(value = "进攻篮板", required = true)
        private Integer offensiveRebounds;

        @ApiModelProperty(value = "防守篮板", required = true)
        private Integer defensiveRebounds;

        @ApiModelProperty(value = "助攻", required = true)
        private Integer assists;

        @ApiModelProperty(value = "抢断", required = true)
        private Integer steals;

        @ApiModelProperty(value = "盖帽", required = true)
        private Integer blocks;

        @ApiModelProperty(value = "失误", required = true)
        private Integer turnovers;

        @ApiModelProperty(value = "犯规", required = true)
        private Integer fouls;

        @ApiModelProperty(value = "投篮命中数", required = true)
        private Integer fieldGoalsMade;

        @ApiModelProperty(value = "投篮出手数", required = true)
        private Integer fieldGoalsAttempted;

        @ApiModelProperty(value = "投篮命中率", required = true)
        private java.math.BigDecimal fieldGoalPct;

        @ApiModelProperty(value = "三分命中数", required = true)
        private Integer threePointsMade;

        @ApiModelProperty(value = "三分出手数", required = true)
        private Integer threePointsAttempted;

        @ApiModelProperty(value = "三分命中率", required = true)
        private java.math.BigDecimal threePointPct;

        @ApiModelProperty(value = "罚球命中数", required = true)
        private Integer freeThrowsMade;

        @ApiModelProperty(value = "罚球出手数", required = true)
        private Integer freeThrowsAttempted;

        @ApiModelProperty(value = "罚球命中率", required = true)
        private java.math.BigDecimal freeThrowPct;

        @ApiModelProperty(value = "正负值", required = true)
        private Integer plusMinus;

        @ApiModelProperty(value = "效率值", required = true)
        private java.math.BigDecimal efficiency;

        @ApiModelProperty(value = "两双标记", required = true)
        private Integer doubleDouble;

        @ApiModelProperty(value = "三双标记", required = true)
        private Integer tripleDouble;

        @ApiModelProperty(value = "数据状态", required = true)
        private Integer dataStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "确认人主键")
        private Long confirmedBy;

        @ApiModelProperty(value = "确认时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date confirmedTime;

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
