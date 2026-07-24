package com.zhs.basket.req;

import lombok.Data;

@Data
public class TournamentCreateReq {
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
    private Integer publicStatus;
    private Integer displayOrder;
}
