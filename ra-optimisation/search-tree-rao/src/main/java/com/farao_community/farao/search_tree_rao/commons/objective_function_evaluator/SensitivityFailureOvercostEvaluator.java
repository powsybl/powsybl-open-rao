/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SensitivityFailureOvercostEvaluator implements CostEvaluator {
    private final double sensitivityFailureOvercost;
    private final Set<State> states;

    public SensitivityFailureOvercostEvaluator(Set<FlowCnec> flowCnecs) {
        this.sensitivityFailureOvercost = 100000;
        this.states = flowCnecs.stream().map(Cnec::getState).collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return "sensitivity-failure-cost";
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>());
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        if (sensitivityStatus == ComputationStatus.FAILURE) {
            return sensitivityFailureOvercost;
        }
        for (State state : states) {
            if (sensitivityResult.getSensitivityStatus(state) == ComputationStatus.FAILURE) {
                return sensitivityFailureOvercost;
            }
        }
        return 0;
    }

    @Override
    public Unit getUnit() {
        return Unit.MEGAWATT;
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int number) {
        return Collections.emptyList();
    }

    @Override
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int number, Set<String> contingenciesToExclude) {
        return Collections.emptyList();
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return Collections.emptySet();
    }
}
