package com.zhs.framework.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 雪花 ID 反序列化：字符串走 Long.parseLong，数字走 BigInteger 避免 double 精度丢失。
 */
public class LongStringDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_STRING) {
            String text = parser.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            return Long.parseLong(text);
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getBigIntegerValue().longValueExact();
        }
        throw context.weirdStringException(parser.getText(), Long.class, "期望 Long 字符串或整数");
    }
}
