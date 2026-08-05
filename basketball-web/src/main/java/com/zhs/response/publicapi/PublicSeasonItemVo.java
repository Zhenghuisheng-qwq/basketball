package com.zhs.response.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("公开赛季项")
public class PublicSeasonItemVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季编号")
    private String seasonNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事主键")
    private Long tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛季年份")
    private String seasonYear;

    @ApiModelProperty("赛季状态")
    private String seasonStatus;

    @ApiModelProperty("报名开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationStartTime;

    @ApiModelProperty("报名结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date registrationEndTime;

    @ApiModelProperty("赛季开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonStartTime;

    @ApiModelProperty("赛季结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonEndTime;

    @ApiModelProperty("最大球队数")
    private Integer maxTeamCount;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("所属顶层赛季主键")
    private Long competitionSeasonId;

    @ApiModelProperty("所属顶层赛季名称")
    private String competitionSeasonName;

    @ApiModelProperty("所属顶层赛季年份")
    private Integer competitionSeasonYear;
}
