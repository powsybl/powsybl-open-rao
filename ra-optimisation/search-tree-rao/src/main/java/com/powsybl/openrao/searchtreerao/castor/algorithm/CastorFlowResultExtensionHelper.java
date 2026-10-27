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
        FlowResult.FlowResultBuilder flowResultBuilder = FlowResult.build();
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResultBuilder, initialResult, flowCnec, null, flowUnit));
        return flowResultBuilder.build();
    }

    public static FlowResult convertToExtension(PrePerimeterResult initialResult, PrePerimeterResult preventiveResult, Crac crac, Unit flowUnit) {
        FlowResult.FlowResultBuilder flowResultBuilder = FlowResult.build();
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResultBuilder, initialResult, flowCnec, null, flowUnit));
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResultBuilder, preventiveResult, flowCnec, crac.getPreventiveInstant(), flowUnit));
        return flowResultBuilder.build();
    }

    public static FlowResult convertToExtension(PrePerimeterResult initialResult,
                                                PrePerimeterResult preventiveResult,
                                                Map<State, PostPerimeterResult> postContingencyResults,
                                                Crac crac,
                                                Unit flowUnit) {
        FlowResult.FlowResultBuilder flowResultBuilder = FlowResult.build();
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResultBuilder, initialResult, flowCnec, null, flowUnit));
        crac.getFlowCnecs().forEach(flowCnec -> fillResultForInstant(flowResultBuilder, preventiveResult, flowCnec, crac.getPreventiveInstant(), flowUnit));
        for (State state : postContingencyResults.keySet()) {
            if (hasRemedialActionsApplied(postContingencyResults.get(state).optimizationResult(), state)) {
                PrePerimeterResult prePerimeterResult = postContingencyResults.get(state).prePerimeterResultForAllFollowingStates();
                crac.getStates(state.getContingency().orElseThrow())
                    .stream()
                    .filter(s -> !s.getInstant().comesBefore(state.getInstant()))
                    .forEach(
                        s -> crac.getFlowCnecs(s).forEach(
                            flowCnec -> fillResultForInstant(flowResultBuilder, prePerimeterResult, flowCnec, state.getInstant(), flowUnit)
                        )
                    );
            }
        }
        return flowResultBuilder.build();
    }

    private static boolean hasRemedialActionsApplied(OptimizationResult optimizationResult, State state) {
        return !optimizationResult.getActivatedNetworkActions().isEmpty() || !optimizationResult.getActivatedRangeActions(state).isEmpty();
    }

    private static void fillResultForInstant(FlowResult.FlowResultBuilder flowResultBuilder, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        fillResultForUnit(flowResultBuilder, prePerimeterResult, flowCnec, instant, Unit.AMPERE);
        fillResultForUnit(flowResultBuilder, prePerimeterResult, flowCnec, instant, Unit.MEGAWATT);
    }

    private static void fillResultForUnit(FlowResult.FlowResultBuilder flowResultBuilder, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        // flows
        safeGetAndAddFlow(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.ONE, flowUnit);
        safeGetAndAddFlow(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.TWO, flowUnit);

        // commercial flows
        safeGetAndAddCommercialFlow(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.ONE, flowUnit);
        safeGetAndAddCommercialFlow(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.TWO, flowUnit);

        // PTDF zonal sums
        safeGetAndAddPtdfZonalSum(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.ONE);
        safeGetAndAddPtdfZonalSum(prePerimeterResult, flowResultBuilder, flowCnec, instant, TwoSides.TWO);
    }

    private static void safeGetAndAddFlow(PrePerimeterResult prePerimeterResult,
                                          FlowResult.FlowResultBuilder flowResultBuilder,
                                          FlowCnec flowCnec,
                                          Instant instant,
                                          TwoSides side,
                                          Unit flowUnit) {
        double flow = prePerimeterResult.getFlow(flowCnec, side, flowUnit, instant);
        if (!Double.isNaN(flow)) {
            flowResultBuilder.addFlowMeasurement(flow, instant, flowCnec, side, flowUnit);
        }
    }

    private static void safeGetAndAddCommercialFlow(PrePerimeterResult prePerimeterResult,
                                                    FlowResult.FlowResultBuilder flowResultBuilder,
                                                    FlowCnec flowCnec,
                                                    Instant instant,
                                                    TwoSides side,
                                                    Unit flowUnit) {
        // FlowResultImpl throws exception if PTDF zonal sums are missing -> maybe consider a future refactoring
        try {
            double commercialFlow = prePerimeterResult.getCommercialFlow(flowCnec, side, flowUnit);
            if (!Double.isNaN(commercialFlow)) {
                flowResultBuilder.addCommercialFlowMeasurement(commercialFlow, instant, flowCnec, side, flowUnit);
            }
        } catch (OpenRaoException ignored) {
        }
    }

    private static void safeGetAndAddPtdfZonalSum(PrePerimeterResult prePerimeterResult,
                                                  FlowResult.FlowResultBuilder flowResultBuilder,
                                                  FlowCnec flowCnec,
                                                  Instant instant,
                                                  TwoSides side) {
        // FlowResultImpl throws exception if PTDF zonal sums are missing -> maybe consider a future refactoring
        try {
            double ptdfZonalSum = prePerimeterResult.getPtdfZonalSum(flowCnec, side);
            if (!Double.isNaN(ptdfZonalSum)) {
                flowResultBuilder.addPtdfZonalSumMeasurement(ptdfZonalSum, instant, flowCnec, side);
            }
        } catch (OpenRaoException ignored) {
        }
    }
}
