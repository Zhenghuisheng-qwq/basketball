package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketSeasonQuery {
    private Long id;
    private Long seriesId;
    private String seriesCode;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String publishStatus;
    private String seasonStatus;
}
