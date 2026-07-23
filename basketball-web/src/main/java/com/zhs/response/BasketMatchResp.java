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
@ApiModel(value = "BasketMatchResp")
public class BasketMatchResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "比赛编号", required = true)
        private String matchNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事赛季主键")
        private Long seasonId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事阶段主键")
        private Long stageId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事轮次主键")
        private Long roundId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事分组主键")
        private Long groupId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "比赛场地主键")
        private Long venueId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主队主键")
        private Long homeTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "客队主键")
        private Long awayTeamId;

        @ApiModelProperty(value = "计划开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date scheduledStartTime;

        @ApiModelProperty(value = "计划结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date scheduledEndTime;

        @ApiModelProperty(value = "实际开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date actualStartTime;

        @ApiModelProperty(value = "实际结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date actualEndTime;

        @ApiModelProperty(value = "比赛状态", required = true)
        private Integer matchStatus;

        @ApiModelProperty(value = "主队得分", required = true)
        private Integer homeScore;

        @ApiModelProperty(value = "客队得分", required = true)
        private Integer awayScore;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "获胜球队主键")
        private Long winnerTeamId;

        @ApiModelProperty(value = "加时次数", required = true)
        private Integer overtimeCount;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "判胜球队主键")
        private Long walkoverTeamId;

        @ApiModelProperty(value = "取消原因")
        private String cancelReason;

        @ApiModelProperty(value = "数据状态", required = true)
        private Integer dataStatus;

        @ApiModelProperty(value = "公开状态", required = true)
        private Integer publicStatus;

        @ApiModelProperty(value = "排序号", required = true)
        private Integer sortNo;

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
