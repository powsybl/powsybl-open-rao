/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.results.FlowResult;
import com.farao_community.farao.rao_api.results.ObjectiveFunctionResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;

import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final FlowResult flowResult;
    private final SensitivityStatus sensitivityStatus;
    private Double functionalCost;
    private Double virtualCost;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       FlowResult flowResult,
                                       SensitivityStatus sensitivityStatus) {
        this.objectiveFunction = objectiveFunction;
        this.flowResult = flowResult;
        this.sensitivityStatus = sensitivityStatus;
    }

    @Override
    public double getFunctionalCost() {
        if (functionalCost == null) {
            functionalCost = objectiveFunction.getFunctionalCost(flowResult, sensitivityStatus);
        }
        return functionalCost;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return objectiveFunction.getMostLimitingElements(flowResult, number);
    }

    @Override
    public double getVirtualCost() {
        if (virtualCost == null) {
            virtualCost = objectiveFunction.getVirtualCost(flowResult, sensitivityStatus);
        }
        return virtualCost;
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
