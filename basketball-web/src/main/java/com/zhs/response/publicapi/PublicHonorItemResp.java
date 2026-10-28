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
@ApiModel("公开荣誉列表项")
public class PublicHonorItemResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("荣誉主键")
    private Long honorId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事主键")
    private Long tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("英文赛季展示名")
    private String seasonNameEn;

    @ApiModelProperty("荣誉编码")
    private String honorCode;

    @ApiModelProperty("荣誉名称")
    private String honorName;

    @ApiModelProperty("荣誉范围")
    private String honorScope;

    @ApiModelProperty("获奖对象类型")
    private String recipientType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("球队主键")
    private Long teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队 Logo")
    private String teamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事球队主键，用于公开球队页")
    private Long eventTeamId;

    @ApiModelProperty("比赛平台")
    private String platformCode;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("球员主键")
    private Long playerId;

    @ApiModelProperty("球员展示名")
    private String playerName;

    @ApiModelProperty("球员头像")
    private String playerAvatarUrl;

    @ApiModelProperty("荣誉标题")
    private String honorTitle;

    @ApiModelProperty("英文荣誉标题")
    private String honorTitleEn;

    @ApiModelProperty("荣誉副标题")
    private String honorSubtitle;

    @ApiModelProperty("英文荣誉副标题")
    private String honorSubtitleEn;

    @ApiModelProperty("荣誉数值")
    private String honorValue;

    @ApiModelProperty("授予日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date awardDate;

    @ApiModelProperty("封面 URL")
    private String coverUrl;

    @ApiModelProperty("展示顺序")
    private Integer displayOrder;
}
