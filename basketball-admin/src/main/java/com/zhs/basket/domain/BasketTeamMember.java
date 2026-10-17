package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTeamMember {
    private Long id;
    private String membershipNo;
    private Long teamId;
    /** 所属赛事 Division ID；NULL 表示 Legacy 历史成员关系 */
    private Long divisionId;
    private Long memberId;
    private Long playerId;
    private String roleType;
    private String rosterType;
    private String jerseyNo;
    private String joinSource;
    private Integer membershipStatus;
    private Integer isCurrent;
    private Date joinTime;
    private Date leaveTime;
    private String leaveReason;
    private String remark;
    private String activeMemberSlot;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
