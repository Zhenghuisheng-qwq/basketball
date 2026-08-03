package com.zhs.request.v4;

import lombok.Data;
import java.util.List;

@Data
public class V4RegistrationSaveReq {
    private Long divisionId;
    private Long teamId;
    private List<Long> playerIds;
}
