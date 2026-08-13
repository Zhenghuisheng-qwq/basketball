package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.vo.v4.ScheduleUnitView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ScheduleFingerprintSupport {

    private ScheduleFingerprintSupport() {}

    public static String fingerprint(List<ScheduleUnitView> units) {
        StringBuilder sb = new StringBuilder();
        units.stream()
                .sorted(Comparator.comparing(ScheduleUnitView::getUnitType, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ScheduleUnitView::getUnitId, Comparator.nullsLast(Long::compareTo)))
                .forEach(u -> sb.append(n(u.getUnitType())).append('|')
                        .append(u.getUnitId() == null ? "" : u.getUnitId()).append('|')
                        .append(n(u.getScheduledDate())).append('|')
                        .append(n(u.getScheduledStartTime())).append('\n'));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("schedule fingerprint failed", e);
        }
    }

    private static String n(String v) {
        return v == null ? "" : v;
    }
}
