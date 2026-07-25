package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AccountPlayerQuery extends BaseEntity {
    private String email;
    private String nickname;
    private String playerNo;
    private String gameId;
    private String gameTerminal;
    /** 账号状态：0禁用 1正常 */
    private Integer accountStatus;
    /** 用户认证阶段：NOT_CERTIFIED/PENDING/APPROVED/REJECTED/NO_PROFILE */
    private String certStage;
    /** 是否已加入球队：1是 0否 */
    private Integer hasTeam;
    /** 是否具备有效队长资格：1是 0否 */
    private Integer hasCaptainQual;
    private List<Integer> statuses;
    private Integer certificationStatus;
    private Integer playerStatus;
}
