package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class PlayerCertListVo {
    private Long id;
    private String applicationNo;
    private Long memberId;
    private String realName;
    private Integer applicationStatus;
    private String applicationStatusName;
    private Date submittedTime;
    private Date reviewedTime;
}
