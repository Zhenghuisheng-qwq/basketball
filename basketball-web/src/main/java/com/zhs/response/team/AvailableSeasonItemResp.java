package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("可报名赛季项")
public class AvailableSeasonItemResp implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季编号")
    private String seasonNo;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季状态")
    private Integer seasonStatus;

    @ApiModelProperty("最小阵容人数")
    private Integer minRosterCount;

    @ApiModelProperty("最大阵容人数")
    private Integer maxRosterCount;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名开始时间")
    private Date registrationStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名结束时间")
    private Date registrationEndTime;

    @ApiModelProperty("是否已存在报名")
    private Boolean hasRegistration;

    @ApiModelProperty("已有报名主键")
    private String registrationId;
}
