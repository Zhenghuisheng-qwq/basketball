package com.zhs.basket.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationGrantReq {
    private Long playerId;
    private Integer paymentConfirmed;
    private String paymentReference;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validTo;

    private String grantRemark;
}
