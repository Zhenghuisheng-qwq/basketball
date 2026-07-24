package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationListVo {
    private Long id;
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private Date grantedTime;
    private Date validTo;
}
