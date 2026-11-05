package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.MailNoticeQueryReq;
import com.zhs.basket.req.MailNoticeRecipientQueryReq;
import com.zhs.basket.req.MailNoticeSaveReq;
import com.zhs.basket.resp.MailNoticeResp;
import com.zhs.basket.service.IBasketMailNoticeService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/mail-notice")
public class BasketMailNoticeController extends BaseController {

    @Autowired
    private IBasketMailNoticeService mailNoticeService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(MailNoticeQueryReq query) {
        startPage();
        return getDataTable(mailNoticeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(mailNoticeService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_QUERY + "')")
    @GetMapping("/{id}/recipients")
    public TableDataInfo recipients(@PathVariable Long id, MailNoticeRecipientQueryReq query) {
        mailNoticeService.selectById(id);
        if (query == null) {
            query = new MailNoticeRecipientQueryReq();
        }
        query.setNoticeId(id);
        startPage();
        return getDataTable(mailNoticeService.selectRecipients(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_ADD + "')")
    @Log(title = "邮件通知", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MailNoticeSaveReq req) {
        return success(mailNoticeService.add(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_EDIT + "')")
    @Log(title = "邮件通知", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody MailNoticeSaveReq req) {
        mailNoticeService.edit(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_REMOVE + "')")
    @Log(title = "邮件通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        mailNoticeService.remove(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.MAIL_NOTICE_SEND + "')")
    @Log(title = "邮件通知", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/send")
    public AjaxResult send(@PathVariable Long id) {
        MailNoticeResp resp = mailNoticeService.prepareSend(id);
        mailNoticeService.dispatch(id);
        return success(resp);
    }
}
