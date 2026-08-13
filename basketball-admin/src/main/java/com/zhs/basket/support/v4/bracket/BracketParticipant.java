package com.zhs.basket.support.v4.bracket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BracketParticipant {

    private Long eventTeamId;
    private Integer seedNo;
    private Integer rankingNo;
    private Integer sourceRank;
    private String sourceType;
}
