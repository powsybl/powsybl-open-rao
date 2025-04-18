/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class BatchAutomatonSimulator {

    public BatchAutomatonSimulator() {
    }

    public OptimizationResult simulate(Network network, Crac crac, State automatonState, PrePerimeterResult prePerimeterResult) {
        List<AutomatonBatch> automatonBatches = groupAutomatonsByBatches(crac, automatonState);
        for (AutomatonBatch automatonBatch : automatonBatches) {
            OptimizationResult postBatchResult = automatonBatch.simulate(network);
            List<FlowCnec> mostLimitingElements = postBatchResult.getMostLimitingElements(1);
            if (!mostLimitingElements.isEmpty() && postBatchResult.getMargin(mostLimitingElements.get(0), Unit.MEGAWATT) >= 0) {
                return postBatchResult;
            }
            // TODO: see how to include automatons from previous batches
        }
        return null; // TODO: final result
    }

    private static List<AutomatonBatch> groupAutomatonsByBatches(Crac crac, State automatonState) {
        Map<Integer, AutomatonBatch> automatonBatches = new HashMap<>();
        crac.getRangeActions(automatonState, UsageMethod.FORCED).forEach(
            automaton -> automatonBatches.computeIfAbsent(automaton.getSpeed().orElse(0), k -> new AutomatonBatch(automaton.getSpeed().orElse(0))).add(automaton)
        );
        return automatonBatches.values().stream().sorted().toList();
    }
}
