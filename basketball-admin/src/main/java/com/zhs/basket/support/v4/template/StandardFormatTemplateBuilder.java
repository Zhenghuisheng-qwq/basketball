package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.SeriesCodeEnum;

public interface StandardFormatTemplateBuilder {
    SeriesCodeEnum support();

    String templateCode();

    StandardTemplateDefinition capabilities();

    void validate(StandardTemplateContext context);

    StandardTemplateDefinition build(StandardTemplateContext context);
}
