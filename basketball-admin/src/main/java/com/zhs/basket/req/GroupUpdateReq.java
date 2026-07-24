package com.zhs.basket.req;

import lombok.Data;

@Data
public class GroupUpdateReq {
    private String groupName;
    private Integer qualifyCount;
    private Integer sequenceNo;
    private Integer groupStatus;
}
