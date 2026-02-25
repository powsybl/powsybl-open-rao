/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CostEvaluatorUtils {

    private CostEvaluatorUtils() {
    }

    public static Map<State, Set<FlowCnec>> groupFlowCnecsPerState(Set<FlowCnec> flowCnecs) {
        Map<State, Set<FlowCnec>> flowCnecsPerState = new HashMap<>();
        flowCnecs.forEach(flowCnec -> flowCnecsPerState.computeIfAbsent(flowCnec.getState(), k -> new HashSet<>()).add(flowCnec));
        return flowCnecsPerState;
    }

    public static List<FlowCnec> sortFlowCnecsByDecreasingCost(Map<FlowCnec, Double> costPerFlowCnec) {
        List<FlowCnec> sortedFlowCnecs = new ArrayList<>(costPerFlowCnec.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).toList());
        Collections.reverse(sortedFlowCnecs);
        return sortedFlowCnecs;
    }
}
