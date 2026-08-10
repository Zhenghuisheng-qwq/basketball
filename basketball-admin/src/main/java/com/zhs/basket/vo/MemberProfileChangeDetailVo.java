package com.zhs.basket.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MemberProfileChangeDetailVo extends MemberProfileChangeListVo {
    private Long oldAvatarFileId;
    private Long newAvatarFileId;
    private Long auditUserId;
    private String auditRemark;
}
