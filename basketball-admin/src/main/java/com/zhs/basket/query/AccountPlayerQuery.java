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
    private List<Integer> statuses;
    private Integer certificationStatus;
    private Integer playerStatus;
}
