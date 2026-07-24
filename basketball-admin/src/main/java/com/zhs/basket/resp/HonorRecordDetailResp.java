package com.zhs.basket.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class HonorRecordDetailResp extends HonorRecordListResp {
    private List<HonorLineupResp> lineup;
}
