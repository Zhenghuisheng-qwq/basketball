package com.zhs.basket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class CaptainGrantSeasonOptionVo {
    private Long seasonId;
    private String seasonName;
    private Integer seasonStatus;
    private String seasonStatusName;
    private Long tournamentId;
    private String tournamentName;
    private String gameTerminal;
    private String competitionSeasonName;
    private Integer competitionSeasonYear;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonEndTime;

    /** 展示用：顶层赛季 / 游戏端 / 赛事名称 */
    private String displayName;
}
