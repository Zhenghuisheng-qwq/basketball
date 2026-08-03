package com.zhs.basket.vo.v4;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasketSeasonDetailVo extends BasketSeasonListVo {
    private List<BasketDivisionListVo> divisions;
}
