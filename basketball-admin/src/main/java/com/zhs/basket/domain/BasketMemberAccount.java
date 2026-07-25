package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMemberAccount {
    private Long id;
    private String accountNo;
    private String username;
    private String email;
    private Integer accountStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
