package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4PublicSeriesItemResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    private String seriesCode;

    private String seriesName;

    private String seriesDesc;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long iconFileId;

    private String iconUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long coverFileId;

    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bannerFileId;

    private String bannerUrl;

    private Integer enabled;

    private Integer seasonCount;
}
