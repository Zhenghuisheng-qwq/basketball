package com.zhs.basket.support.v4.formation;

import com.zhs.basket.support.v4.roster.GameModeRosterRule;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FormationContext {
    private Long divisionId;
    private Long ruleVersionId;
    private GameModeRosterRule rosterRule;
    private List<FormationPlayer> validPlayers = new ArrayList<>();
    private Long randomSeed;
}
