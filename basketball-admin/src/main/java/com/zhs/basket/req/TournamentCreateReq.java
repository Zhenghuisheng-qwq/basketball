package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class TournamentCreateReq {
    private Long competitionSeasonId;
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
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date competitionStartTime;
    private Date competitionEndTime;
    private Integer publicStatus;
    private Integer displayOrder;
}
