package com.powsybl.openrao.commons;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CounterTradeGeneratorOrdering(
        List<GlskGeneratorAllocation> upwardOrder,
        List<GlskGeneratorAllocation> downwardOrder,
        int upwardLastStage,
        double maxUpwardMagnitude,
        int downwardLastStage,
        double maxDownwardMagnitude,
        Map<String, Double> initialTargetP,
        Set<String> zeroWeightGeneratorIds) { }
