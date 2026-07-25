package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerAccountVo {
    private Long memberId;
    private String accountNo;
    private String username;
    private String email;
    private Integer accountStatus;
    private Date createTime;
    private Date updateTime;
}
