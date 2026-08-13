package com.zhs.basket.support.v4.schedule;

import com.zhs.common.exception.ServiceException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public final class ScheduleTimes {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT_SHORT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FMT_LONG = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private ScheduleTimes() {}

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ServiceException("比赛日期不能为空");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new ServiceException("比赛日期格式无效");
        }
    }

    public static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            throw new ServiceException("开始时间不能为空");
        }
        String raw = value.trim();
        try {
            if (raw.length() == 5) {
                return LocalTime.parse(raw, TIME_FMT_SHORT);
            }
            return LocalTime.parse(raw, TIME_FMT_LONG);
        } catch (DateTimeParseException ex) {
            throw new ServiceException("开始时间格式无效");
        }
    }

    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    public static String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FMT_SHORT);
    }

    public static Date toDate(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            return null;
        }
        return Date.from(LocalDateTime.of(date, time).atZone(ZONE).toInstant());
    }

    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZONE).toLocalDate();
    }

    public static LocalTime toLocalTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZONE).toLocalTime().withSecond(0).withNano(0);
    }
}
