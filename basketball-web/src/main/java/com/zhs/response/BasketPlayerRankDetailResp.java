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
@ApiModel(value = "BasketPlayerRankDetailResp")
public class BasketPlayerRankDetailResp implements Serializable {
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

        @ApiModelProperty(value = "排行类型", required = true)
        private String rankType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球员主键")
        private Long playerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "球队主键")
        private Long teamId;

        @ApiModelProperty(value = "当前排名", required = true)
        private Integer rankNo;

        @ApiModelProperty(value = "上期排名")
        private Integer previousRank;

        @ApiModelProperty(value = "排行指标值", required = true)
        private java.math.BigDecimal statValue;

        @ApiModelProperty(value = "快照日期")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date snapshotDate;

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
