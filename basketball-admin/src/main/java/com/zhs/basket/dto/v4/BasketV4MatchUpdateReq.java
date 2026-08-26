package com.zhs.basket.dto.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BasketV4MatchUpdateReq {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date matchTime;
    private String venueText;
    private String remark;
    private String replayUrl;
}
