package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class PlayerCertListVo {
    private Long id;
    private String applicationNo;
    private Long memberId;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String gameTerminal;
    private String gameAccount;
    private String positionCode;
    private Integer applicationStatus;
    private String applicationStatusName;
    private Date submittedTime;
    private Date reviewedTime;
}
