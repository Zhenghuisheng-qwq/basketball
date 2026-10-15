package com.zhs.request.v4;

import lombok.Data;

import java.util.List;

@Data
public class V4RosterChangeSubmitReq {
    private Long registrationId;
    private List<Long> playerIds;
}
