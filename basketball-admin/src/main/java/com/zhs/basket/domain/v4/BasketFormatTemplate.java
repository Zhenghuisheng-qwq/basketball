package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketFormatTemplate {
    private Long id;
    private Long seriesId;
    private String templateCode;
    private String templateName;
    private Integer versionNo;
    private String templateDescription;
    private Integer enabled;
    private Integer isDefault;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
