package com.zhs.basket.localdemo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单次 seed 执行过程中的上下文，保存各阶段生成的业务主键。
 */
@Data
public class BasketLocalDemoSeedContextDto {

    private Long placeholderFileId;
    private final Map<Integer, Long> accountIds = new HashMap<>();
    private final Map<Integer, Long> memberProfileIds = new HashMap<>();
    private final Map<Integer, Long> certApplicationIds = new HashMap<>();
    private final Map<Integer, Long> playerIds = new HashMap<>();
    private final Map<String, Long> teamIds = new HashMap<>();
    private final Map<String, Long> captainQualificationIds = new HashMap<>();
    private final Map<String, Long> registrationIds = new HashMap<>();
    private Long tournamentId;
    private Long seasonId;
    private Long stageId;
    private final Map<Integer, Long> roundIds = new HashMap<>();
    private Long groupId;
    private final Map<Integer, Long> matchIds = new HashMap<>();
    private final List<Long> announcementIds = new ArrayList<>();
    private final List<Long> bannerIds = new ArrayList<>();
}
