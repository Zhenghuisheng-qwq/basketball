package com.zhs.basket.query;

import lombok.Data;

@Data
public class OpenRegistrationAuditQuery {
    private Long seasonId;
    private String registrationNo;
    private Integer registrationStatus;
}
