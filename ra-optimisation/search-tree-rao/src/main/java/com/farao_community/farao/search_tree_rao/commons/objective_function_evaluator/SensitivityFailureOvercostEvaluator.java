/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluator implements CostEvaluator {
    private final double sensitivityFailureOvercost;
    private final Set<State> states;

    public SensitivityFailureOvercostEvaluator(Set<FlowCnec> flowCnecs, double sensitivityFailureOvercost) {
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
        this.states = flowCnecs.stream().map(Cnec::getState).collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return "sensitivity-failure-cost";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        if (sensitivityStatus == ComputationStatus.FAILURE) {
            TECHNICAL_LOGS.info(String.format("Sensitivity failure : assigning virtual overcost of %s", sensitivityFailureOvercost));
            return Pair.of(sensitivityFailureOvercost, new ArrayList<>());
        }
        for (State state : states) {
            Optional<Contingency> contingency = state.getContingency();
            if ((state.getContingency().isEmpty() || contingency.isPresent() && !contingenciesToExclude.contains(contingency.get().getId())) &&
                    sensitivityResult.getSensitivityStatus(state) == ComputationStatus.FAILURE) {
                TECHNICAL_LOGS.info(String.format("Sensitivity failure for state %s : assigning virtual overcost of %s", state.getId(), sensitivityFailureOvercost));
                return Pair.of(sensitivityFailureOvercost, new ArrayList<>());
            }
        }
        return Pair.of(0., new ArrayList<>());
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
