package com.zhs.basket.support.v4.formation;

public interface FormationStrategy {
    String type();

    FormationPlan plan(FormationContext context);
}
