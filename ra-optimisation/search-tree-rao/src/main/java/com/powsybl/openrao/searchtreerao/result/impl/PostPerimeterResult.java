package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class PostPerimeterResult {

    private final OptimizationResult optimizationResult;
    private final PrePerimeterResult prePerimeterResultForAllFollowingStates;

    public PostPerimeterResult(OptimizationResult optimizationResult, PrePerimeterResult prePerimeterResultForAllFollowingStates) {
        this.optimizationResult = optimizationResult;
        this.prePerimeterResultForAllFollowingStates = prePerimeterResultForAllFollowingStates;
    }

    public OptimizationResult getOptimizationResult() {
        return optimizationResult;
    }

    public PrePerimeterResult getPrePerimeterResultForAllFollowingStates() {
        return prePerimeterResultForAllFollowingStates;
    }
}
