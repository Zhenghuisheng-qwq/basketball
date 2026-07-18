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
@ApiModel(value = "BasketTournamentDto")
public class BasketTournamentDto implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛季编号")
        private Long seasonId;

        @ApiModelProperty(value = "赛事编号", required = true)
        private String tournamentNo;

        @ApiModelProperty(value = "赛事名称", required = true)
        private String tournamentName;

        @ApiModelProperty(value = "赛事类型", required = true)
        private String tournamentType;

        @ApiModelProperty(value = "比赛形式", required = true)
        private String matchFormat;

        @ApiModelProperty(value = "比赛终端", required = true)
        private String terminalType;

        @ApiModelProperty(value = "封面地址")
        private String coverUrl;

        @ApiModelProperty(value = "赛事简介")
        private String intro;

        @ApiModelProperty(value = "报名开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date signStartTime;

        @ApiModelProperty(value = "报名结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date signEndTime;

        @ApiModelProperty(value = "比赛开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date matchStartTime;

        @ApiModelProperty(value = "比赛结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date matchEndTime;

        @ApiModelProperty(value = "最少人数", required = true)
        private Integer minMemberCount;

        @ApiModelProperty(value = "最多人数", required = true)
        private Integer maxMemberCount;

        @ApiModelProperty(value = "首发人数", required = true)
        private Integer starterCount;

        @ApiModelProperty(value = "替补人数", required = true)
        private Integer substituteCount;

        @ApiModelProperty(value = "队伍上限")
        private Integer teamLimit;

        @ApiModelProperty(value = "报名数量", required = true)
        private Integer registrationCount;

        @ApiModelProperty(value = "发布标志", required = true)
        private String publishFlag;

        @ApiModelProperty(value = "赛事状态", required = true)
        private String tournamentStatus;

        @ApiModelProperty(value = "排序号", required = true)
        private Integer sortNo;

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

        @ApiModelProperty(value = "备注")
        private String remark;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
