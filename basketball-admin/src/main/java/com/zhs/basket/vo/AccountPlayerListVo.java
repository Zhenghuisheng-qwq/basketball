package com.zhs.basket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class AccountPlayerListVo {
    /** 用户ID（历史字段 memberId，业务含义为登录账号主键） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String accountNo;
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer emailVerified;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    private String playerNo;
    private String gameTerminal;
    private String gameAccount;
    /** 有效平台游戏 ID 列表（展示用） */
    private List<PlatformGameIdItemVo> platformGameIds;
    /** 完整摘要，如 PC：xxx / PS：yyy */
    private String gameIdSummary;
    /** 表格紧凑摘要 */
    private String gameIdCompactSummary;
    private Integer certificationStatus;
    private String certificationStatusName;
    private Integer latestApplicationStatus;
    private String certDisplayStatus;
    private String certDisplayStatusName;
    private Integer playerStatus;
    private Integer accountStatus;
    private String accountStatusName;
    private Long currentTeamId;
    private String currentTeamName;
    /** 是否具备有效队长授权：1是 0否 */
    private Integer hasCaptainQual;
    /** 当前有效队长授权赛事名称 */
    private String activeCaptainGrantEventName;
    private Date lastLoginTime;
    private Date certifiedTime;
    private Date createTime;
}
