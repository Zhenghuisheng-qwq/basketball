package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TournamentListVo {
    private Long id;
    private String tournamentNo;
    private String tournamentName;
    private Integer tournamentStatus;
    private String tournamentStatusName;
    private Integer displayOrder;
    private Date createTime;
}
