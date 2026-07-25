package com.zhs.basket.vo;

import lombok.Data;

@Data
public class TournamentDetailVo extends TournamentListVo {
    private String shortName;
    private String englishName;
    private Long logoFileId;
    private Long coverFileId;
    private String tournamentType;
    private String formatCode;
    private String organizerName;
    private String description;
    private Integer publicStatus;
}
