/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

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
        // FIXME: this is potentially really slow and could use a speed-up
        FlowResult flowResult = convertToExtension(initialResult, preventiveResult, crac, flowUnit);
        crac.getFlowCnecs()
            .stream()
            .filter(flowCnec -> flowCnec.getState().getInstant().isAuto() || flowCnec.getState().getInstant().isCurative())
            .forEach(flowCnec -> {
                Pair<Instant, PrePerimeterResult> instantAndPrePerimeterResult = getLastComputedResult(
                    flowCnec.getState(),
                    preventiveResult,
                    postContingencyResults,
                    crac.getPreventiveInstant()
                );
                crac.getSortedInstants()
                    .stream()
                    .filter(instant -> !instant.comesBefore(instantAndPrePerimeterResult.getLeft())
                        && !instant.comesAfter(flowCnec.getState().getInstant()))
                    .forEach(instant -> fillResultForInstant(flowResult, instantAndPrePerimeterResult.getRight(), flowCnec, instant, flowUnit));

            });
        return flowResult;
    }

    private static void fillResultForInstant(FlowResult flowResult, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        flowResult.addFlowMeasurement(prePerimeterResult.getFlow(flowCnec, TwoSides.ONE, flowUnit), instant, flowCnec, TwoSides.ONE, flowUnit);
        flowResult.addFlowMeasurement(prePerimeterResult.getFlow(flowCnec, TwoSides.TWO, flowUnit), instant, flowCnec, TwoSides.TWO, flowUnit);
        flowResult.addCommercialFlowMeasurement(prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.ONE, flowUnit), instant, flowCnec, TwoSides.ONE, flowUnit);
        flowResult.addCommercialFlowMeasurement(prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.TWO, flowUnit), instant, flowCnec, TwoSides.TWO, flowUnit);
        if (flowUnit == Unit.MEGAWATT) {
            flowResult.addPtdfZonalSumMeasurement(prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.ONE), instant, flowCnec, TwoSides.ONE);
            flowResult.addPtdfZonalSumMeasurement(prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.TWO), instant, flowCnec, TwoSides.TWO);
        }
    }

    private static Pair<Instant, PrePerimeterResult> getLastComputedResult(State state,
                                                                           PrePerimeterResult preventiveResult,
                                                                           Map<State, PostPerimeterResult> postContingencyResults,
                                                                           Instant preventiveInstant) {
        if (postContingencyResults.containsKey(state)) {
            return Pair.of(state.getInstant(), postContingencyResults.get(state).prePerimeterResultForAllFollowingStates());
        }
        Optional<State> lastStateWithResult = postContingencyResults.keySet()
            .stream()
            .filter(s -> s.getContingency().equals(state.getContingency()))
            .filter(s -> s.getInstant().comesBefore(state.getInstant()))
            .max(Comparator.comparing(State::getInstant));
        Instant instant = lastStateWithResult.map(State::getInstant).orElse(preventiveInstant);
        PrePerimeterResult prePerimeterResult = lastStateWithResult.map(s -> postContingencyResults.get(s).prePerimeterResultForAllFollowingStates())
            .orElse(preventiveResult);
        return Pair.of(instant, prePerimeterResult);
    }
}
