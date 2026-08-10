package com.zhs.basket.query;

import lombok.Data;

@Data
public class MemberProfileChangeQuery {
    private String changeNo;
    private String nickname;
    private String submitUser;
    private Integer auditStatus;
    private String auditStage;
    private String beginTime;
    private String endTime;
}
