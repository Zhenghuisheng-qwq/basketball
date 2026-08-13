package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StagePairingSeedReq {
    private List<Long> seedEventTeamIds = new ArrayList<>();
}
