package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerNotificationVo {
    private Long receiverId;
    private String title;
    private String messageType;
    private String bizType;
    private Integer readStatus;
    private String readStatusName;
    private Date createTime;
}
