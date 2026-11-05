package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMailNotice;
import com.zhs.basket.domain.BasketMailNoticeRecipient;
import com.zhs.basket.mapper.BasketMailNoticeMapper;
import com.zhs.basket.mapper.BasketMailNoticeRecipientMapper;
import com.zhs.basket.req.MailNoticeQueryReq;
import com.zhs.basket.req.MailNoticeRecipientQueryReq;
import com.zhs.basket.req.MailNoticeSaveReq;
import com.zhs.basket.resp.MailNoticeRecipientResp;
import com.zhs.basket.resp.MailNoticeResp;
import com.zhs.basket.service.IBasketMailNoticeService;
import com.zhs.basket.support.BasketDataMaskUtil;
import com.zhs.basket.support.mail.AdminHtmlMailSender;
import com.zhs.common.exception.ServiceException;
import jakarta.mail.MessagingException;
import com.zhs.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BasketMailNoticeServiceImpl implements IBasketMailNoticeService {

    private static final int TITLE_MAX = 120;
    private static final int CONTENT_MAX = 200_000;
    private static final int NAME_MAX = 128;
    private static final int INSERT_BATCH = 200;
    /** 每分钟匀速发出的封数。间隔按「上一封开始」到「下一封开始」计算。 */
    private static final int SENDS_PER_MINUTE = 10;
    private static final long SEND_INTERVAL_MS = 60_000L / SENDS_PER_MINUTE;
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_SENDING = 1;
    private static final int RECIPIENT_SUCCESS = 1;
    private static final int RECIPIENT_FAIL = 2;

    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern IFRAME = Pattern.compile("(?is)<iframe[^>]*>.*?</iframe>");
    private static final Pattern OBJECT = Pattern.compile("(?is)<object[^>]*>.*?</object>");
    private static final Pattern EMBED = Pattern.compile("(?is)<embed\\b[^>]*>");
    private static final Pattern EVENT_ATTR = Pattern.compile("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JS_PROTOCOL = Pattern.compile("(?i)javascript:");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private final ExecutorService senderPool = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mail-notice-sender");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Long> runningNoticeIds = ConcurrentHashMap.newKeySet();

    @Autowired
    private BasketMailNoticeMapper noticeMapper;
    @Autowired
    private BasketMailNoticeRecipientMapper recipientMapper;
    @Autowired
    private AdminHtmlMailSender mailSender;

    @Override
    public List<MailNoticeResp> selectList(MailNoticeQueryReq query) {
        return noticeMapper.selectList(query == null ? new MailNoticeQueryReq() : query);
    }

    @Override
    public MailNoticeResp selectById(Long id) {
        MailNoticeResp detail = noticeMapper.selectDetail(id);
        if (detail == null) {
            throw new ServiceException("通知不存在");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(MailNoticeSaveReq req) {
        BasketMailNotice notice = new BasketMailNotice();
        notice.setTitle(requireTitle(req));
        notice.setContentHtml(requireContent(req));
        notice.setCreateBy(SecurityUtils.getUsername());
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, MailNoticeSaveReq req) {
        BasketMailNotice existing = requireDraft(id);
        existing.setTitle(requireTitle(req));
        existing.setContentHtml(requireContent(req));
        if (noticeMapper.updateDraft(existing) == 0) {
            throw new ServiceException("只能修改未发送的草稿");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireDraft(id);
        if (noticeMapper.markDeleted(id) == 0) {
            throw new ServiceException("只能删除未发送的草稿");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailNoticeResp prepareSend(Long id) {
        BasketMailNotice notice = noticeMapper.selectByIdForUpdate(id);
        if (notice == null) {
            throw new ServiceException("通知不存在");
        }
        Integer status = notice.getSendStatus();
        if (status != null && status == STATUS_SENDING) {
            throw new ServiceException("该通知正在发送");
        }
        if (status == null || status != STATUS_DRAFT) {
            throw new ServiceException("该通知已经发送，不能重复发送");
        }
        if (!mailSender.isConfigured()) {
            throw new ServiceException("邮件服务未配置，暂时不能发送");
        }
        List<BasketMailNoticeRecipient> recipients = dedupeRecipients(id);
        if (recipients.isEmpty()) {
            throw new ServiceException("没有可发送的已注册邮箱");
        }
        insertInChunks(recipients);
        if (noticeMapper.markSending(id, recipients.size()) == 0) {
            throw new ServiceException("该通知状态已变化，请刷新后重试");
        }
        MailNoticeResp detail = noticeMapper.selectDetail(id);
        if (detail == null) {
            throw new ServiceException("通知不存在");
        }
        return detail;
    }

    @Override
    public void dispatch(Long id) {
        senderPool.execute(() -> sendPending(id));
    }

    @Override
    public List<MailNoticeRecipientResp> selectRecipients(MailNoticeRecipientQueryReq query) {
        if (query == null || query.getNoticeId() == null) {
            throw new ServiceException("通知不存在");
        }
        return recipientMapper.selectList(query);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinished() {
        try {
            List<Long> ids = noticeMapper.selectSendingIds();
            if (ids == null || ids.isEmpty()) {
                return;
            }
            log.info("[mail-notice] resume unfinished count={}", ids.size());
            for (Long id : ids) {
                dispatch(id);
            }
        } catch (Exception ex) {
            log.error("[mail-notice] resume skipped ex={}", ex.getClass().getName());
        }
    }

    private void sendPending(Long noticeId) {
        BasketMailNotice notice = noticeMapper.selectById(noticeId);
        if (notice == null || notice.getSendStatus() == null || notice.getSendStatus() != STATUS_SENDING) {
            return;
        }
        if (!runningNoticeIds.add(noticeId)) {
            return;
        }
        try {
            List<BasketMailNoticeRecipient> pending = recipientMapper.selectPending(noticeId);
            if (pending == null || pending.isEmpty()) {
                return;
            }
            if (!mailSender.isConfigured()) {
                for (BasketMailNoticeRecipient item : pending) {
                    recipientMapper.markResult(item.getId(), RECIPIENT_FAIL, "邮件服务未配置");
                    noticeMapper.refreshCounts(noticeId);
                }
                return;
            }
            String subject = notice.getTitle() == null ? "CNPA通知" : notice.getTitle().replaceAll("[\\r\\n]+", " ").trim();
            String html = buildMailHtml(subject, notice.getContentHtml());
            log.info("[mail-notice] start noticeId={} recipients={} pace={}/min intervalMs={}",
                    noticeId, pending.size(), SENDS_PER_MINUTE, SEND_INTERVAL_MS);
            for (int i = 0; i < pending.size(); i++) {
                long started = System.nanoTime();
                sendOne(noticeId, subject, html, pending.get(i));
                if (i == pending.size() - 1) {
                    break;
                }
                long spentMs = (System.nanoTime() - started) / 1_000_000L;
                long waitMs = SEND_INTERVAL_MS - spentMs;
                if (waitMs > 0 && !pause(waitMs)) {
                    log.info("[mail-notice] pace interrupted noticeId={}", noticeId);
                    break;
                }
            }
        } catch (Exception ex) {
            log.error("[mail-notice] send loop failed noticeId={} ex={}", noticeId, ex.getClass().getName());
        } finally {
            runningNoticeIds.remove(noticeId);
            noticeMapper.refreshCounts(noticeId);
            noticeMapper.markCompletedIfNoPending(noticeId);
        }
    }

    private String mailFailureReason(Throwable error) {
        StringBuilder text = new StringBuilder();
        appendFailure(text, error, 0);
        if (text.length() == 0) {
            text.append(error.getClass().getName());
        }
        return text.toString();
    }

    private void appendFailure(StringBuilder text, Throwable current, int depth) {
        if (current == null || depth >= 8) {
            return;
        }
        String message = current.getMessage();
        if (message != null && !message.isBlank()) {
            String one = message.replaceAll("[\\r\\n]+", " ").trim();
            if (!one.isEmpty() && text.indexOf(one) < 0) {
                if (text.length() > 0) {
                    text.append(" | ");
                }
                text.append(current.getClass().getName()).append(": ").append(one);
            }
        }
        appendFailure(text, current.getCause(), depth + 1);
        if (current instanceof MessagingException messaging
                && messaging.getNextException() != null
                && messaging.getNextException() != current.getCause()) {
            appendFailure(text, messaging.getNextException(), depth + 1);
        }
    }

    private boolean pause(long waitMs) {
        try {
            Thread.sleep(waitMs);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void sendOne(Long noticeId, String subject, String html, BasketMailNoticeRecipient item) {
        String email = item.getEmail() == null ? "" : item.getEmail().trim();
        try {
            if (!email.contains("@")) {
                recipientMapper.markResult(item.getId(), RECIPIENT_FAIL, "邮箱格式不正确");
            } else {
                mailSender.sendHtml(email, subject, html);
                recipientMapper.markResult(item.getId(), RECIPIENT_SUCCESS, null);
            }
        } catch (Exception ex) {
            String reason = mailFailureReason(ex);
            log.error("[mail-notice] send failed noticeId={} to={} reason={}",
                    noticeId, BasketDataMaskUtil.maskEmail(email), reason, ex);
            recipientMapper.markResult(item.getId(), RECIPIENT_FAIL, cut(reason, 240));
        }
        noticeMapper.refreshCounts(noticeId);
    }

    private List<BasketMailNoticeRecipient> dedupeRecipients(Long noticeId) {
        List<BasketMailNoticeRecipient> source = recipientMapper.selectSendableMembers();
        Map<String, BasketMailNoticeRecipient> unique = new LinkedHashMap<>();
        if (source == null) {
            return List.of();
        }
        for (BasketMailNoticeRecipient row : source) {
            if (row == null || row.getMemberId() == null || !StringUtils.hasText(row.getEmail())) {
                continue;
            }
            String email = row.getEmail().trim();
            String key = email.toLowerCase(Locale.ROOT);
            if (unique.containsKey(key)) {
                continue;
            }
            row.setNoticeId(noticeId);
            row.setEmail(cut(email, 255));
            row.setUsername(cut(row.getUsername(), NAME_MAX));
            row.setNickname(cut(row.getNickname(), NAME_MAX));
            row.setSendStatus(0);
            unique.put(key, row);
        }
        return new ArrayList<>(unique.values());
    }

    private void insertInChunks(List<BasketMailNoticeRecipient> recipients) {
        for (int i = 0; i < recipients.size(); i += INSERT_BATCH) {
            int end = Math.min(i + INSERT_BATCH, recipients.size());
            recipientMapper.insertBatch(recipients.subList(i, end));
        }
    }

    private BasketMailNotice requireDraft(Long id) {
        BasketMailNotice existing = noticeMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("通知不存在");
        }
        if (existing.getSendStatus() == null || existing.getSendStatus() != STATUS_DRAFT) {
            throw new ServiceException("只能操作未发送的草稿");
        }
        return existing;
    }

    private String requireTitle(MailNoticeSaveReq req) {
        if (req == null || !StringUtils.hasText(req.getTitle())) {
            throw new ServiceException("标题不能为空");
        }
        String title = req.getTitle().trim();
        if (title.length() > TITLE_MAX) {
            throw new ServiceException("标题不能超过" + TITLE_MAX + "个字");
        }
        return title;
    }

    private String requireContent(MailNoticeSaveReq req) {
        if (req == null || !StringUtils.hasText(req.getContentHtml())) {
            throw new ServiceException("通知内容不能为空");
        }
        String content = sanitize(req.getContentHtml());
        if (content.length() > CONTENT_MAX) {
            throw new ServiceException("通知内容过长");
        }
        String text = TAG.matcher(content).replaceAll("")
                .replace("&nbsp;", " ")
                .replace("\u00a0", " ")
                .trim();
        boolean hasImage = content.toLowerCase(Locale.ROOT).contains("<img");
        if (!hasImage && !StringUtils.hasText(text)) {
            throw new ServiceException("通知内容不能为空");
        }
        return content;
    }

    private String sanitize(String html) {
        String cleaned = html;
        cleaned = SCRIPT.matcher(cleaned).replaceAll("");
        cleaned = STYLE.matcher(cleaned).replaceAll("");
        cleaned = IFRAME.matcher(cleaned).replaceAll("");
        cleaned = OBJECT.matcher(cleaned).replaceAll("");
        cleaned = EMBED.matcher(cleaned).replaceAll("");
        cleaned = EVENT_ATTR.matcher(cleaned).replaceAll("");
        cleaned = JS_PROTOCOL.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    private String buildMailHtml(String subject, String contentHtml) {
        String body = contentHtml == null ? "" : contentHtml.replace("<img", "<img style=\"max-width:100%;height:auto;\"");
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"margin:0;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#222;line-height:1.7;\">"
                + "<h2 style=\"margin:0 0 16px;font-size:20px;\">" + escapeHtml(subject) + "</h2>"
                + body
                + "</body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String cut(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
