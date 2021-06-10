package com.farao_community.farao.rao_result_io;

import com.farao_community.farao.rao_api.results.OptimizationState;

import java.util.HashMap;
import java.util.Map;

public class FlowCnecResult {

    private static final ElementaryFlowCnecResult DEFAULT_RESULT = new ElementaryFlowCnecResult();
    private Map<OptimizationState, ElementaryFlowCnecResult> results;

    public ElementaryFlowCnecResult getResult(OptimizationState optimizationState) {
        return results.getOrDefault(optimizationState, DEFAULT_RESULT);
    }

    public FlowCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryFlowCnecResult addElementaryResult(OptimizationState optimizationState) {
        results.putIfAbsent(optimizationState, new ElementaryFlowCnecResult());
        return results.get(optimizationState);
    }
}
