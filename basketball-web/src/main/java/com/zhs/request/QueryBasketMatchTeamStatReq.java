package com.zhs.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
public class QueryBasketMatchTeamStatReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long matchId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long opponentTeamId;

        private Integer points;

        private Integer rebounds;

        private Integer offensiveRebounds;

        private Integer defensiveRebounds;

        private Integer assists;

        private Integer steals;

        private Integer blocks;

        private Integer turnovers;

        private Integer fouls;

        private Integer fieldGoalsMade;

        private Integer fieldGoalsAttempted;

        private java.math.BigDecimal fieldGoalPct;

        private Integer threePointsMade;

        private Integer threePointsAttempted;

        private java.math.BigDecimal threePointPct;

        private Integer freeThrowsMade;

        private Integer freeThrowsAttempted;

        private java.math.BigDecimal freeThrowPct;

        private java.math.BigDecimal possessions;

        private java.math.BigDecimal efficiency;

        private Integer dataStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long confirmedBy;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date confirmedTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
