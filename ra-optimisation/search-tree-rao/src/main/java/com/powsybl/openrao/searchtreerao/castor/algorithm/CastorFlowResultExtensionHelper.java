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
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;

import java.util.List;
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
        // FIXME: this is potentially really slow and could use a speed-up
        FlowResult flowResult = convertToExtension(initialResult, preventiveResult, crac, flowUnit);
        for (State state : postContingencyResults.keySet()) {
            PrePerimeterResult prePerimeterResult = postContingencyResults.get(state).prePerimeterResultForAllFollowingStates();
            List<State> followingStates = crac.getStates(state.getContingency().orElseThrow())
                .stream()
                .filter(s -> !s.getInstant().comesBefore(state.getInstant()))
                .toList();
            followingStates.forEach(
                s -> crac.getFlowCnecs(s).forEach(
                    flowCnec -> fillResultForInstant(flowResult, prePerimeterResult, flowCnec, state.getInstant(), flowUnit)
                )
            );
        }
        return flowResult;
    }

    private static void fillResultForInstant(FlowResult flowResult, PrePerimeterResult prePerimeterResult, FlowCnec flowCnec, Instant instant, Unit flowUnit) {
        // TODO: note to future self, please fix this
        try {
            flowResult.addFlowMeasurement(prePerimeterResult.getFlow(flowCnec, TwoSides.ONE, flowUnit), instant, flowCnec, TwoSides.ONE, flowUnit);
        } catch (OpenRaoException ignored) {
        }
        try {
            flowResult.addFlowMeasurement(prePerimeterResult.getFlow(flowCnec, TwoSides.TWO, flowUnit), instant, flowCnec, TwoSides.TWO, flowUnit);
        } catch (OpenRaoException ignored) {
        }
        try {
            flowResult.addCommercialFlowMeasurement(prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.ONE, flowUnit), instant, flowCnec, TwoSides.ONE, flowUnit);
        } catch (OpenRaoException ignored) {
        }
        try {
            flowResult.addCommercialFlowMeasurement(prePerimeterResult.getCommercialFlow(flowCnec, TwoSides.TWO, flowUnit), instant, flowCnec, TwoSides.TWO, flowUnit);
        } catch (OpenRaoException ignored) {
        }
        if (flowUnit == Unit.MEGAWATT) {
            try {
                flowResult.addPtdfZonalSumMeasurement(prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.ONE), instant, flowCnec, TwoSides.ONE);
            } catch (OpenRaoException ignored) {
            }
            try {
                flowResult.addPtdfZonalSumMeasurement(prePerimeterResult.getPtdfZonalSum(flowCnec, TwoSides.TWO), instant, flowCnec, TwoSides.TWO);
            } catch (OpenRaoException ignored) {
            }
        }
    }
}
