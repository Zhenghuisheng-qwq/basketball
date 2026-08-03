package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchSubmissionVersionVo {
    private Long submissionId;
    private Integer versionNo;
    private String submissionStatus;
    private Date submitTime;
    private Date auditTime;
}
