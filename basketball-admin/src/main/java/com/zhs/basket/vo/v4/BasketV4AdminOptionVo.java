package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketV4AdminOptionVo {
    private Long id;
    private String label;
    private String subLabel;
    private String platformCode;
    private Integer eventTeamCount;
    private Long formatSnapshotId;
}
