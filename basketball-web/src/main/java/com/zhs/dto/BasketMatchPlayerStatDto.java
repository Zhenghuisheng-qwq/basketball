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
@ApiModel(value = "BasketMatchPlayerStatDto")
public class BasketMatchPlayerStatDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "比赛编号")
        private Long matchId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事编号")
        private Long tournamentId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事球队编号")
        private Long tournamentTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事球员编号")
        private Long tournamentPlayerId;

        @ApiModelProperty(value = "球员名称", required = true)
        private String playerName;

        @ApiModelProperty(value = "首发标志", required = true)
        private String starterFlag;

        @ApiModelProperty(value = "出场分钟", required = true)
        private Integer playMinutes;

        @ApiModelProperty(value = "得分", required = true)
        private Integer points;

        @ApiModelProperty(value = "篮板", required = true)
        private Integer rebounds;

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

        @ApiModelProperty(value = "三分命中", required = true)
        private Integer threePoints;

        @ApiModelProperty(value = "罚球命中", required = true)
        private Integer freeThrows;

        @ApiModelProperty(value = "投篮命中", required = true)
        private Integer fieldGoals;

        @ApiModelProperty(value = "数据状态", required = true)
        private String statStatus;

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
