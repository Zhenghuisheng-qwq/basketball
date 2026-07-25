package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerPlatformChangeQuery extends BaseEntity {
    private String requestNo;
    private String nickname;
    private String email;
    private String playerNo;
    private String platform;
    private String changeType;
    private Integer requestStatus;
    private Long memberId;
}
