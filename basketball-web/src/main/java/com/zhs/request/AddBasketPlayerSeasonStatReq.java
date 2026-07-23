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
public class AddBasketPlayerSeasonStatReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long teamId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long playerId;

        private Integer games;

        private Integer starts;

        private Integer minutesSeconds;

        private Integer points;

        private Integer rebounds;

        private Integer assists;

        private Integer steals;

        private Integer blocks;

        private Integer turnovers;

        private Integer fouls;

        private java.math.BigDecimal avgMinutes;

        private java.math.BigDecimal avgPoints;

        private java.math.BigDecimal avgRebounds;

        private java.math.BigDecimal avgAssists;

        private java.math.BigDecimal avgSteals;

        private java.math.BigDecimal avgBlocks;

        private java.math.BigDecimal fieldGoalPct;

        private java.math.BigDecimal threePointPct;

        private java.math.BigDecimal freeThrowPct;

        private java.math.BigDecimal avgEfficiency;

        private Integer doubleDoubleCount;

        private Integer tripleDoubleCount;

        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date lastCalculatedTime;

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
