package com.zhs.support;

import com.zhs.common.cachekey.member.MemberPasswordAuthVersionCacheKey;
import com.zhs.common.cachekey.member.MemberTokenCacheKey;
import com.zhs.config.MemberAuthProperties;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.model.BasketMemberAccount;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 会员登录会话：token 存储、密码变更后全端失效。
 */
@Component
public class MemberSessionSupport {

    private static final String TOKEN_VALUE_SEPARATOR = "|";

    @Resource
    private MiniRedisUtil miniRedisUtil;
    @Resource
    private MemberAuthProperties memberAuthProperties;
    @Resource
    private BasketMemberAccountDao memberAccountDao;

    public ParsedTokenSession resolveTokenSession(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Object cached = miniRedisUtil.get(memberTokenKey(token));
        return parseTokenSession(cached);
    }

    public void storeToken(String token, Long memberId, long authVersion) {
        miniRedisUtil.set(memberTokenKey(token), buildTokenValue(memberId, authVersion));
    }

    public void revokeToken(String token) {
        if (StringUtils.hasText(token)) {
            miniRedisUtil.delete(memberTokenKey(token));
        }
    }

    public ParsedTokenSession parseTokenSession(Object cached) {
        if (cached == null) {
            return null;
        }
        String raw = String.valueOf(cached).trim();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        int separatorIndex = raw.indexOf(TOKEN_VALUE_SEPARATOR);
        if (separatorIndex < 0) {
            Long memberId = parseLong(raw);
            return memberId == null ? null : new ParsedTokenSession(memberId, 0L);
        }
        Long memberId = parseLong(raw.substring(0, separatorIndex));
        Long authVersion = parseLong(raw.substring(separatorIndex + 1));
        if (memberId == null || authVersion == null) {
            return null;
        }
        return new ParsedTokenSession(memberId, authVersion);
    }

    public long resolveAuthVersion(BasketMemberAccount account) {
        if (account == null || account.getPasswordUpdateTime() == null) {
            return 0L;
        }
        return account.getPasswordUpdateTime().getTime();
    }

    public void syncAuthVersion(Long memberId, long authVersion) {
        if (memberId == null) {
            return;
        }
        miniRedisUtil.set(new MemberPasswordAuthVersionCacheKey(memberId), String.valueOf(authVersion));
    }

    public long getRequiredAuthVersion(Long memberId) {
        if (memberId == null) {
            return 0L;
        }
        Object cached = miniRedisUtil.get(new MemberPasswordAuthVersionCacheKey(memberId));
        Long parsed = parseLong(cached);
        if (parsed != null) {
            return parsed;
        }
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        long version = resolveAuthVersion(account);
        syncAuthVersion(memberId, version);
        return version;
    }

    public boolean isTokenAuthVersionValid(Long memberId, long tokenAuthVersion) {
        return tokenAuthVersion >= getRequiredAuthVersion(memberId);
    }

    /**
     * 修改密码后：同步鉴权版本并使当前 token 失效（其他端旧 token 因版本不一致而失效）。
     */
    public void invalidateAllSessions(Long memberId, String currentToken, long authVersion) {
        syncAuthVersion(memberId, authVersion);
        revokeToken(currentToken);
    }

    private MemberTokenCacheKey memberTokenKey(String token) {
        return new MemberTokenCacheKey(token, memberAuthProperties.getTokenExpireDays());
    }

    private String buildTokenValue(Long memberId, long authVersion) {
        return String.valueOf(memberId) + TOKEN_VALUE_SEPARATOR + authVersion;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        return null;
    }

    public record ParsedTokenSession(Long memberId, Long authVersion) {
    }
}
