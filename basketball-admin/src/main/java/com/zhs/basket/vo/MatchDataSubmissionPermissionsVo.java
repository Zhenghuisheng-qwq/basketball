package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionPermissionsVo {
    private Boolean canApprove;
    private Boolean canReject;
    private Boolean canViewMaterial;
}
