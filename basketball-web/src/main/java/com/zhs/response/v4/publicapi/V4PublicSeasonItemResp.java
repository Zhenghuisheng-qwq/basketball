package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class V4PublicSeasonItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    private String seriesCode;

    private String seriesName;

    private Integer seasonYear;

    private String seasonName;

    private String displayTitle;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long coverFileId;

    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bannerFileId;

    private String bannerUrl;

    private String registrationMode;

    private String formatType;

    private Date registrationStartTime;

    private Date registrationEndTime;

    private Date matchStartTime;

    private Date matchEndTime;

    private String seasonStatus;

    /** 已发布赛区数量 */
    private Integer divisionCount;

    /** 赛区涉及平台（去重） */
    private List<String> platformCodes;

    /** 赛区涉及比赛模式（去重） */
    private List<String> gameModes;

    /** 赛区涉及报名方式（去重） */
    private List<String> registrationModes;

    /** 当前报名中的赛区数量 */
    private Integer registrationOpenDivisionCount;

    /** 最近一场报名截止时间（报名中赛区） */
    private Date nearestRegistrationEndTime;

    private List<V4PublicDivisionBriefResp> divisions;
}
