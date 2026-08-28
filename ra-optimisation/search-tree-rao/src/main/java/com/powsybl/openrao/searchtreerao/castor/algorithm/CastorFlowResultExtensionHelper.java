/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;

import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CastorFlowResultExtensionHelper {

    private CastorFlowResultExtensionHelper() {
    }

    public static FlowResult convertToExtension(PrePerimeterResult initialResult, Crac crac, Unit flowUnit) {
        FlowResult flowResult = new FlowResult();
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResult, initialResult, flowCnec, null, flowUnit));
        return flowResult;
    }

    public static FlowResult convertToExtension(PrePerimeterResult initialResult, PrePerimeterResult preventiveResult, Crac crac, Unit flowUnit) {
        FlowResult flowResult = convertToExtension(initialResult, crac, flowUnit);
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResult, preventiveResult, flowCnec, crac.getPreventiveInstant(), flowUnit));
        return flowResult;
    }

    public static FlowResult convertToExtension(PrePerimeterResult initialResult,
                                                PrePerimeterResult preventiveResult,
                                                Map<State, PostPerimeterResult> postContingencyResults,
                                                Crac crac,
                                                Unit flowUnit) {
        FlowResult flowResult = convertToExtension(initialResult, preventiveResult, crac, flowUnit);
        for (State state : postContingencyResults.keySet()) {
            if (hasRemedialActionsApplied(postContingencyResults.get(state).optimizationResult(), state)) {
                PrePerimeterResult prePerimeterResult = postContingencyResults.get(state).prePerimeterResultForAllFollowingStates();
                crac.getStates(state.getContingency().orElseThrow())
                    .stream()
                    .filter(s -> !s.getInstant().comesBefore(state.getInstant()))
                    .forEach(
                        s -> crac.getFlowCnecs(s).forEach(
                            flowCnec -> fillResultForInstant(flowResult, prePerimeterResult, flowCnec, state.getInstant(), flowUnit)
                        )
                    );
            }
        }
        return flowResult;
    }

    private static boolean hasRemedialActionsApplied(OptimizationResult optimizationResult, State state) {
        return !optimizationResult.getActivatedNetworkActions().isEmpty() || !optimizationResult.getActivatedRangeActions(state).isEmpty();
    }

    private static void fillResultForInstant(FlowResult flowResult, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        fillResultForUnit(flowResult, prePerimeterResult, flowCnec, instant, Unit.AMPERE);
        fillResultForUnit(flowResult, prePerimeterResult, flowCnec, instant, Unit.MEGAWATT);
    }

    private static void fillResultForUnit(FlowResult flowResult, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        // TODO: check zero-flows
        // TODO: note to future self, please fix this
        double flow1 = prePerimeterResult.getFlow(flowCnec, TwoSides.ONE, flowUnit);
        if (!Double.isNaN(flow1)) {
            flowResult.addFlowMeasurement(flow1, instant, flowCnec, TwoSides.ONE, flowUnit);
        }
        double flow2 = prePerimeterResult.getFlow(flowCnec, TwoSides.TWO, flowUnit);
        if (!Double.isNaN(flow2)) {
            flowResult.addFlowMeasurement(flow2, instant, flowCnec, TwoSides.TWO, flowUnit);
        }
        try {
            double commercialFlow1 = prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.ONE, flowUnit);
            if (!Double.isNaN(commercialFlow1)) {
                flowResult.addCommercialFlowMeasurement(commercialFlow1, instant, flowCnec, TwoSides.ONE, flowUnit);
            }
        } catch (OpenRaoException ignored) {
        }
        try {
            double commercialFlow2 = prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.TWO, flowUnit);
            if (!Double.isNaN(commercialFlow2)) {
                flowResult.addCommercialFlowMeasurement(commercialFlow2, instant, flowCnec, TwoSides.TWO, flowUnit);
            }
        } catch (OpenRaoException ignored) {
        }
        try {
            double ptdfZonalSum1 = prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.ONE);
            if (!Double.isNaN(ptdfZonalSum1)) {
                flowResult.addPtdfZonalSumMeasurement(ptdfZonalSum1, instant, flowCnec, TwoSides.ONE);
            }
        } catch (OpenRaoException ignored) {
        }
        try {
            double ptdfZonalSum2 = prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.TWO);
            if (!Double.isNaN(ptdfZonalSum2)) {
                flowResult.addPtdfZonalSumMeasurement(ptdfZonalSum2, instant, flowCnec, TwoSides.TWO);
            }
        } catch (OpenRaoException ignored) {
        }
    }
}
