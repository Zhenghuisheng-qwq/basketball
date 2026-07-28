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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date seasonEndTime;

    /** 展示用：赛事名称 / 赛季名称 */
    private String displayName;
}
