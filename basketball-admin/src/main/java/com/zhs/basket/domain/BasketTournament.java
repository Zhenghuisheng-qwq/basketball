package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournament {
    private Long id;
    private String tournamentNo;
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
    private java.util.Date registrationStartTime;
    private java.util.Date registrationEndTime;
    private java.util.Date competitionStartTime;
    private java.util.Date competitionEndTime;
    private Integer tournamentStatus;
    private Integer publicStatus;
    private Integer displayOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
