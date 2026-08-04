package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class V4PublicStageStandingScopeResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupScopeKey;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    private String groupCode;
    private String groupName;
    private Integer qualifyCount;
    private List<V4PublicStandingItemResp> rows = new ArrayList<>();
}
