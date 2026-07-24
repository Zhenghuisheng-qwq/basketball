package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.MessageItemResp;
import com.zhs.service.IAccountMessageService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/message")
@Api(value = "账户消息")
@Slf4j
public class AccountMessageController {

    @Resource
    private IAccountMessageService accountMessageService;

    @ApiOperation("分页查询当前会员消息")
    @GetMapping("/page")
    public R<IPage<MessageItemResp>> pageMessages(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer readStatus) {
        return R.ok(accountMessageService.pageMessages(currentPage, pageSize, readStatus));
    }

    @ApiOperation("查询未读消息数量")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(accountMessageService.countUnread());
    }

    @ApiOperation("查询消息详情")
    @GetMapping("/{receiverId}")
    public R<MessageItemResp> getMessage(@PathVariable Long receiverId) {
        return R.ok(accountMessageService.getMessage(receiverId));
    }

    @ApiOperation("标记单条消息已读")
    @PutMapping("/{receiverId}/read")
    public R<Void> markRead(@PathVariable Long receiverId) {
        accountMessageService.markRead(receiverId);
        return R.ok(null, "标记已读成功");
    }

    @ApiOperation("标记全部消息已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        accountMessageService.markAllRead();
        return R.ok(null, "全部标记已读成功");
    }
}
