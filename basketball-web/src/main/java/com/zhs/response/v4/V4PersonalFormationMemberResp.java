package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4PersonalFormationMemberResp {
    private Long assignmentId;
    private Long playerId;
    private String playerName;
    private String assignmentRole;
    private String joinStatus;
    private Integer sortOrder;
}
