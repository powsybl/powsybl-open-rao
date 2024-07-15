/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluator implements CostEvaluator {
    private final double sensitivityFailureOvercost;
    private final Set<State> states;
    private final Set<FlowCnec> flowCnecs;

    public SensitivityFailureOvercostEvaluator(Set<FlowCnec> flowCnecs, double sensitivityFailureOvercost) {
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
        this.states = flowCnecs.stream().map(Cnec::getState).collect(Collectors.toSet());
        this.flowCnecs = flowCnecs;
    }

    @Override
    public String getName() {
        return "sensitivity-failure-cost";
    }

    @Override
    public Pair<Double, Map<FlowCnec, Double>> computeCostAndLimitingElements(FlowResult flowResult, SensitivityResult sensitivityResult, Set<String> contingenciesToExclude) {
        if (sensitivityResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
            TECHNICAL_LOGS.info(String.format("Sensitivity failure : assigning virtual overcost of %s", sensitivityFailureOvercost));
            FlowCnec preventiveFlowCnec = flowCnecs.stream().filter(c -> c.getState().isPreventive()).iterator().next();
            return Pair.of(sensitivityFailureOvercost, Map.of(preventiveFlowCnec, sensitivityFailureOvercost));
        }
        Map<FlowCnec, Double> cnecMap = new HashMap<>();
        for (State state : states) {
            Optional<Contingency> contingency = state.getContingency();
            if ((state.getContingency().isEmpty() || contingency.isPresent()) &&
                    sensitivityResult.getSensitivityStatus(state) == ComputationStatus.FAILURE) {
                TECHNICAL_LOGS.info(String.format("Sensitivity failure for state %s : assigning virtual overcost of %s", state.getId(), sensitivityFailureOvercost));
                FlowCnec stateFlowCnec = flowCnecs.stream().filter(c -> c.getState().equals(state)).iterator().next();
                cnecMap.put(stateFlowCnec, sensitivityFailureOvercost);
            }
        }
        if (cnecMap.isEmpty()) {
            return Pair.of(0., new HashMap<>());
        } else {
            return Pair.of(sensitivityFailureOvercost, cnecMap);
        }
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return Collections.emptySet();
    }
}
