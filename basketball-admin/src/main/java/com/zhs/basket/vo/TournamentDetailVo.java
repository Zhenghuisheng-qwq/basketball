package com.zhs.basket.vo;

import lombok.Data;

@Data
public class TournamentDetailVo {
    private Long id;
    private String tournamentNo;
    private String tournamentName;
    private String shortName;
    private String englishName;
    private Long logoFileId;
    private Long coverFileId;
    private String tournamentType;
    private String gameTerminal;
    private String formatCode;
    private String organizerName;
    private String description;
    private Integer tournamentStatus;
    private String tournamentStatusName;
    private Integer publicStatus;
    private Integer displayOrder;
}
