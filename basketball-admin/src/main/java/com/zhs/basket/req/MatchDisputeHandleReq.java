package com.zhs.basket.req;

import lombok.Data;

@Data
public class MatchDisputeHandleReq {
    private String handleResult;
    private String handleRemark;
    private Long revisionId;
}
