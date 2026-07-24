package com.zhs.basket.localdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoPlayerDto {
    private int globalIndex;
    private String teamKey;
    private String name;
    private String positionCode;
    private String jerseyNo;
    private boolean captain;
}
