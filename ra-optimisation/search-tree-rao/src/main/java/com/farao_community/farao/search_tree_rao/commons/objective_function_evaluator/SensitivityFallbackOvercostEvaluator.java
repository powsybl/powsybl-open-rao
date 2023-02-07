/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityFallbackOvercostEvaluator implements CostEvaluator {
    private final double fallBackOvercost;

    public SensitivityFallbackOvercostEvaluator(double overcost) {
        this.fallBackOvercost = overcost;
    }

    @Override
    public String getName() {
        return "sensitivity-fallback-cost";
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, new HashSet<>());
    }

    @Override
    public double computeCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        switch (sensitivityStatus) {
            case FALLBACK:
                return fallBackOvercost;
            case DEFAULT:
            case FAILURE:
            default:
                return 0;
        }
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
    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int numberOfElements, Set<String> contingenciesToExclude) {
        return Collections.emptyList();
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return Collections.emptySet();
    }
}
