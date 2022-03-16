package com.farao_community.farao.search_tree_rao.linear_optimisation.parameters;

import java.util.Map;
import java.util.Set;

public class RangeActionLimitationParameters {

    private final int maxCurativeRangeAction;
    private final int maxCurativeTso;
    private final Set<String> maxCurativeTsoExclusion;
    private final Map<String, Integer> maxCurativePstPerTso;
    private final Map<String, Integer> maxCurativeRangeActionPerTso;

    maxRangeActions
    maxTso
        TsoExclusion
    maxRangeActionPerTso
            maxPstPerTso

    public RangeActionLimitationParameters(int maxCurativeRangeAction,
                                           int maxCurativeTso,
                                           Set<String> maxCurativeTsoExclusion, Map<String, Integer> maxCurativePstPerTso,
                                           Map<String, Integer> maxCurativeRangeActionPerTso) {
        this.maxCurativeRangeAction = maxCurativeRangeAction;
        this.maxCurativeTso = maxCurativeTso;
        this.maxCurativeTsoExclusion = maxCurativeTsoExclusion;
        this.maxCurativePstPerTso = maxCurativePstPerTso;
        this.maxCurativeRangeActionPerTso = maxCurativeRangeActionPerTso;
    }

    public int getMaxCurativeRangeAction() {
        return maxCurativeRangeAction;
    }

    public int getMaxCurativeTso() {
        return maxCurativeTso;
    }

    public Set<String> getMaxCurativeTsoExclusion() {
        return maxCurativeTsoExclusion;
    }

    public Map<String, Integer> getMaxCurativePstPerTso() {
        return maxCurativePstPerTso;
    }

    public Map<String, Integer> getMaxCurativeRangeActionPerTso() {
        return maxCurativeRangeActionPerTso;
    }
}
