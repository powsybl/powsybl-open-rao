/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;

import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final FlowResult flowResult;
    private final ComputationStatus sensitivityStatus;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       FlowResult flowResult,
                                       ComputationStatus sensitivityStatus) {
        this.objectiveFunction = objectiveFunction;
        this.flowResult = flowResult;
        this.sensitivityStatus = sensitivityStatus;
    }

    @Override
    public double getFunctionalCost() {
        return objectiveFunction.getFunctionalCost(flowResult, sensitivityStatus);
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return objectiveFunction.getMostLimitingElements(flowResult, number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunction.getVirtualCost(flowResult, sensitivityStatus);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunction.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunction.getVirtualCost(flowResult, sensitivityStatus, virtualCostName);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunction.getCostlyElements(flowResult, virtualCostName, number);
    }
}
