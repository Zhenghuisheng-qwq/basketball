package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class RoundUpdateReq {
    private String roundName;
    private String roundType;
    private Integer sequenceNo;
    private Integer bestOf;
    private Date startTime;
    private Date endTime;
}
