package com.zhs.basket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.Date;
import lombok.Data;

@Data
public class PlayerCertListVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String applicationNo;
    @JsonSerialize(using = ToStringSerializer.class)
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
