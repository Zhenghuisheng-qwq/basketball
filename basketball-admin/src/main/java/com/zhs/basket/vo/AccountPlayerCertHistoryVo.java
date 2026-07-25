package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerCertHistoryVo {
    private Long applicationId;
    private String applicationNo;
    private String gameTerminal;
    private String gameAccount;
    private Integer applicationStatus;
    private String applicationStatusName;
    private Date submittedTime;
    private Date reviewedTime;
    private String reviewRemark;
}
