package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.SeriesCodeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class StandardFormatTemplateFactory {

    private final Map<SeriesCodeEnum, StandardFormatTemplateBuilder> bySeries = new EnumMap<>(SeriesCodeEnum.class);
    private final Map<String, StandardFormatTemplateBuilder> byCode;

    public StandardFormatTemplateFactory(List<StandardFormatTemplateBuilder> builders) {
        Map<String, StandardFormatTemplateBuilder> codeMap = new java.util.HashMap<>();
        for (StandardFormatTemplateBuilder builder : builders) {
            bySeries.put(builder.support(), builder);
            codeMap.put(builder.templateCode(), builder);
        }
        this.byCode = Map.copyOf(codeMap);
    }

    public StandardFormatTemplateBuilder builderForSeries(String seriesCode) {
        SeriesCodeEnum series = SeriesCodeEnum.fromCode(seriesCode);
        if (series == null) {
            throw new ServiceException("seriesCode is required");
        }
        StandardFormatTemplateBuilder builder = bySeries.get(series);
        if (builder == null) {
            throw new ServiceException("No STANDARD template for series " + seriesCode);
        }
        return builder;
    }

    public StandardFormatTemplateBuilder builderForTemplateCode(String templateCode) {
        StandardFormatTemplateBuilder builder = byCode.get(templateCode);
        if (builder == null) {
            throw new ServiceException("Unknown STANDARD template: " + templateCode);
        }
        return builder;
    }

    public List<StandardTemplateDefinition> catalog(String seriesCode) {
        return bySeries.values().stream()
                .map(StandardFormatTemplateBuilder::capabilities)
                .toList();
    }

    public String defaultTemplateCode(String seriesCode) {
        return builderForSeries(seriesCode).templateCode();
    }
}
