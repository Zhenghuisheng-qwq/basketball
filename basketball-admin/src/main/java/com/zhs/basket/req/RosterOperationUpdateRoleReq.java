package com.zhs.basket.req;

import lombok.Data;

@Data
public class RosterOperationUpdateRoleReq {
    private Long rosterId;
    private String rosterRole;
    private Integer isStarting;
    private Integer isSubstitute;
    private String operateReason;
}
