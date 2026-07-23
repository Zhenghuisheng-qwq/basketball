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
@ApiModel(value = "BasketTournamentSeasonDetailResp")
public class BasketTournamentSeasonDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "赛季编号", required = true)
        private String seasonNo;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "赛事主键")
        private Long tournamentId;

        @ApiModelProperty(value = "赛季名称", required = true)
        private String seasonName;

        @ApiModelProperty(value = "赛季年份", required = true)
        private Integer seasonYear;

        @ApiModelProperty(value = "报名开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date registrationStartTime;

        @ApiModelProperty(value = "报名结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date registrationEndTime;

        @ApiModelProperty(value = "赛季开始时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date seasonStartTime;

        @ApiModelProperty(value = "赛季结束时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date seasonEndTime;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主场地主键")
        private Long venueId;

        @ApiModelProperty(value = "最大球队数量", required = true)
        private Integer maxTeamCount;

        @ApiModelProperty(value = "最小阵容人数", required = true)
        private Integer minRosterCount;

        @ApiModelProperty(value = "最大阵容人数", required = true)
        private Integer maxRosterCount;

        @ApiModelProperty(value = "赛季状态", required = true)
        private Integer seasonStatus;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "当前阶段主键")
        private Long currentStageId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "冠军球队主键")
        private Long championTeamId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "亚军球队主键")
        private Long runnerUpTeamId;

        @ApiModelProperty(value = "公开状态", required = true)
        private Integer publicStatus;

        @ApiModelProperty(value = "赛季说明")
        private Object description;

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
