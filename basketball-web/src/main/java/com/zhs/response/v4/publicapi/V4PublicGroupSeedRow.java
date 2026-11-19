package com.zhs.response.v4.publicapi;

import lombok.Data;

@Data
public class V4PublicGroupSeedRow {
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Integer groupOrder;
    private Integer qualifyCount;
    private Long eventTeamId;
    private String teamName;
    private Long teamLogoFileId;
}
