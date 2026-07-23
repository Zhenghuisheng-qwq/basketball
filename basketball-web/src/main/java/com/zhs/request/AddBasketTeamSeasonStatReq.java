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
public class AddBasketTeamSeasonStatReq implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        private Long teamId;

        private Integer games;

        private Integer wins;

        private Integer losses;

        private Integer pointsFor;

        private Integer pointsAgainst;

        private java.math.BigDecimal avgPoints;

        private java.math.BigDecimal avgPointsAgainst;

        private java.math.BigDecimal avgRebounds;

        private java.math.BigDecimal avgAssists;

        private java.math.BigDecimal avgSteals;

        private java.math.BigDecimal avgBlocks;

        private java.math.BigDecimal avgTurnovers;

        private java.math.BigDecimal fieldGoalPct;

        private java.math.BigDecimal threePointPct;

        private java.math.BigDecimal freeThrowPct;

        private java.math.BigDecimal winRate;

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
