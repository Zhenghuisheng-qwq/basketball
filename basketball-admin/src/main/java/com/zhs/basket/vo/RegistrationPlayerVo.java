package com.zhs.basket.vo;

import lombok.Data;

@Data
public class RegistrationPlayerVo {
    private Long id;
    private Long playerId;
    /** 用户ID（历史字段 memberId） */
    private Long memberId;
    private Long userId;
    private String playerNickname;
    private String playerNo;
    private String rosterRole;
    /** 场上位置编码：PG/SG/SF/PF/C */
    private String positionCode;
    /** 场上位置名称 */
    private String positionName;
    /** 场上位置展示：PG：控球后卫 */
    private String positionText;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarting;
    private Integer validationStatus;
    /** 校验状态名称 */
    private String validationStatusName;
    private String rejectReason;
}
