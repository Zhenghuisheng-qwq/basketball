package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("批量初始化账号结果")
public class MemberInitAccountsResp implements Serializable {

    @ApiModelProperty("本次创建数量")
    private int createdCount;

    @ApiModelProperty("数据文件路径")
    private String dataFilePath;

    @ApiModelProperty("创建明细")
    private List<MemberInitAccountItemResp> accounts = new ArrayList<>();
}
