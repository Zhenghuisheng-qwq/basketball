package com.zhs.service.support;

import com.zhs.enums.PublicPartnerSupportTypeEnum;
import com.zhs.response.publicapi.PublicPartnerSupportRecordResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class PartnerAmountFormatter {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private PartnerAmountFormatter() {
    }

    public static void fillDisplay(PublicPartnerSupportRecordResp record) {
        if (record == null) {
            return;
        }
        PublicPartnerSupportTypeEnum type = PublicPartnerSupportTypeEnum.fromCode(record.getSupportType());
        record.setSupportTypeText(type.getDescription());
        if (type == PublicPartnerSupportTypeEnum.CASH) {
            BigDecimal amount = record.getSupportAmount() == null ? BigDecimal.ZERO : record.getSupportAmount();
            record.setSupportDisplayText("¥" + MONEY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP)));
        } else {
            record.setSupportDisplayText(type.getDescription());
        }
        if (record.getSupportDate() == null) {
            record.setSupportDate("");
        }
        if (record.getPartnerName() == null) {
            record.setPartnerName("");
        }
    }

    public static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
