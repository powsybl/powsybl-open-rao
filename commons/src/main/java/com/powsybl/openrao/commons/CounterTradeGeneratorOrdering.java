package com.powsybl.openrao.commons;

import java.util.List;
import java.util.Set;

public record CounterTradeGeneratorOrdering(
        List<GlskGeneratorAllocation> upwardOrder,
        List<GlskGeneratorAllocation> downwardOrder,
        Set<String> zeroWeightGeneratorIds) { }
