package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationGrantReq {
    private Long playerId;
    private Integer paymentConfirmed;
    private String paymentReference;
    private Date validFrom;
    private Date validTo;
    private String grantRemark;
}
