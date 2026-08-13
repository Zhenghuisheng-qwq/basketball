package com.zhs.basket.support.v4.formation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormationTeamPlan {
    private int teamIndex;
    private String defaultTeamCode;
    private String defaultTeamName;
    private List<FormationPlayer> players = new ArrayList<>();
}
