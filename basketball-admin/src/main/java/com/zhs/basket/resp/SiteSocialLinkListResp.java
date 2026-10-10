package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class SiteSocialLinkListResp {
    private Long id;
    private String platformCode;
    private String platformName;
    private String title;
    private String titleEn;
    private String subtitle;
    private String subtitleEn;
    private String buttonText;
    private String buttonTextEn;
    private String jumpUrl;
    private Integer sortOrder;
    private Integer status;
    private String statusName;
    private Date createTime;
    private Date updateTime;
}
