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

    private List<V4PublicDivisionBriefResp> divisions;
}
