package com.powsybl.openrao.commons;

import java.util.List;
import java.util.Set;

public record CounterTradeGeneratorOrdering(
        List<GlskGeneratorAllocation> upwardOrder,
        List<GlskGeneratorAllocation> downwardOrder,
        int upwardLastStage,
        int downwardLastStage,
        Set<String> zeroWeightGeneratorIds) { }
