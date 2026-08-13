package com.zhs.basket.support.v4.bracket;

import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParticipantSourceResolverRegistry {

    @Autowired
    private List<ParticipantSourceResolver> resolvers;

    public ParticipantSourceResolver resolve(String sourceType) {
        for (ParticipantSourceResolver resolver : resolvers) {
            if (resolver.supports(sourceType)) {
                return resolver;
            }
        }
        throw new ServiceException("不支持的参赛来源: " + sourceType);
    }
}
