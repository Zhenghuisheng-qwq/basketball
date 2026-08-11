package com.zhs.response.v4;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class V4RegistrationRosterPlayerResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamMemberId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;
    private String playerNo;
    private String displayName;
    private String roleType;
    private String roleTypeText;
    private String platformCode;
    private String platformGameId;
    private String platformIdentityStatus;
    private Boolean eligible;
    private String reasonCode;
    private String reasonMessage;
    private Boolean captain;
}
