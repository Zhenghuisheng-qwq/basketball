package com.zhs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataMaskUtilEmailTest {

    @Test
    void masksLongLocalPart() {
        assertEquals("123****789@qq.com", DataMaskUtil.maskEmail("123456789@qq.com"));
    }
}
