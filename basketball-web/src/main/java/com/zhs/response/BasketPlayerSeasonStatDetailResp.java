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
@ApiModel(value = "BasketPlayerSeasonStatDetailResp")
public class BasketPlayerSeasonStatDetailResp implements Serializable {
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

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员主键")
        private Long playerId;

        @ApiModelProperty(value = "比赛场次", required = true)
        private Integer games;

        @ApiModelProperty(value = "首发场次", required = true)
        private Integer starts;

        @ApiModelProperty(value = "总出场秒数", required = true)
        private Integer minutesSeconds;

        @ApiModelProperty(value = "总得分", required = true)
        private Integer points;

        @ApiModelProperty(value = "总篮板", required = true)
        private Integer rebounds;

        @ApiModelProperty(value = "总助攻", required = true)
        private Integer assists;

        @ApiModelProperty(value = "总抢断", required = true)
        private Integer steals;

        @ApiModelProperty(value = "总盖帽", required = true)
        private Integer blocks;

        @ApiModelProperty(value = "总失误", required = true)
        private Integer turnovers;

        @ApiModelProperty(value = "总犯规", required = true)
        private Integer fouls;

        @ApiModelProperty(value = "场均出场分钟", required = true)
        private java.math.BigDecimal avgMinutes;

        @ApiModelProperty(value = "场均得分", required = true)
        private java.math.BigDecimal avgPoints;

        @ApiModelProperty(value = "场均篮板", required = true)
        private java.math.BigDecimal avgRebounds;

        @ApiModelProperty(value = "场均助攻", required = true)
        private java.math.BigDecimal avgAssists;

        @ApiModelProperty(value = "场均抢断", required = true)
        private java.math.BigDecimal avgSteals;

        @ApiModelProperty(value = "场均盖帽", required = true)
        private java.math.BigDecimal avgBlocks;

        @ApiModelProperty(value = "投篮命中率", required = true)
        private java.math.BigDecimal fieldGoalPct;

        @ApiModelProperty(value = "三分命中率", required = true)
        private java.math.BigDecimal threePointPct;

        @ApiModelProperty(value = "罚球命中率", required = true)
        private java.math.BigDecimal freeThrowPct;

        @ApiModelProperty(value = "场均效率值", required = true)
        private java.math.BigDecimal avgEfficiency;

        @ApiModelProperty(value = "两双次数", required = true)
        private Integer doubleDoubleCount;

        @ApiModelProperty(value = "三双次数", required = true)
        private Integer tripleDoubleCount;

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
