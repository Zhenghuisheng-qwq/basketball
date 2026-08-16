package com.zhs.basket.support.restriction;

import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SystemDisciplineClock implements DisciplineClock {
    @Override
    public Date now() {
        return new Date();
    }
}
