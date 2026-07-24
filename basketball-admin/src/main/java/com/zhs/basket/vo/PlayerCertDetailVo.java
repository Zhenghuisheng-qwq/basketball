package com.zhs.basket.vo;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class PlayerCertDetailVo {
    private Long id;
    private Long applicationId;
    private String applicationNo;
    private Long memberId;
    private String realName;
    private String idType;
    private String idNoMasked;
    private String gameTerminal;
    private String gameAccount;
    private String gameNickname;
    private String positionCode;
    private String contactMobileMasked;
    private Integer applicationStatus;
    private String applicationStatusName;
    private String reviewRemark;
    private Date submittedTime;
    private Date reviewedTime;
    private Long currentAuditorId;
    private List<PlayerCertMaterialVo> materials;
    private List<PlayerCertAuditRecordVo> auditRecords;
}
