package com.zhs.basket.support.v4.formation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormationPlan {
    private Long randomSeed;
    private String strategyType;
    private List<FormationTeamPlan> teams = new ArrayList<>();
}
