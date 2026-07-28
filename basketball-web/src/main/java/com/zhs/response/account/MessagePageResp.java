package com.zhs.response.account;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("账户消息分页响应")
public class MessagePageResp extends Page<MessageItemResp> {

    @ApiModelProperty("当前登录用户全部通知数量")
    private Long allCount;

    @ApiModelProperty("当前登录用户未读通知数量")
    private Long unreadCount;

    public MessagePageResp() {
    }

    public MessagePageResp(long current, long size, long total) {
        super(current, size, total);
    }
}
