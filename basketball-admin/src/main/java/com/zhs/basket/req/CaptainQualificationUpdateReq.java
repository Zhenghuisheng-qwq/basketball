package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationUpdateReq {
    private Integer paymentConfirmed;
    private String paymentReference;
    private Date validFrom;
    private Date validTo;
    private String grantRemark;
}
