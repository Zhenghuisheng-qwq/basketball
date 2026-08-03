package com.zhs.basket.vo.v4;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasketScheduleBatchDetailVo extends BasketScheduleBatchListVo {
    private String batchNo;
    private String generateType;
    private List<BasketV4MatchListVo> matches;
}
