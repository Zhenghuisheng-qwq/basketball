package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BracketPreviewVo {
    private Integer participantCount;
    private Integer bracketCapacity;
    private Integer byeCount;
    private Integer firstRoundActual;
    private Integer totalActualSeries;
    private Integer roundCount;
    private List<Round> rounds = new ArrayList<>();

    @Data
    public static class Round {
        private Integer roundNo;
        private String roundName;
        private String boRole;
        private Integer bestOf;
        private List<Pairing> pairings = new ArrayList<>();
    }

    @Data
    public static class Pairing {
        private Integer slot;
        private SlotView home;
        private SlotView away;
        private Boolean byeAdvance;
        private String nextRoundLabel;
    }

    @Data
    public static class SlotView {
        private Integer slotNo;
        private Long eventTeamId;
        private String teamName;
        private Integer seedNo;
        private Integer rankingNo;
        private Boolean bye;
        private String sourceLabel;
        private String sourceType;
        private Long sourceGroupId;
        private String sourceGroupCode;
        private Integer sourceRank;
        private String sourceDisplay;
        private String displayCode;
        private String displayName;
        private String resolutionStatus;
        private Long resolvedEventTeamId;
        private String resolvedEventTeamName;
    }
}
