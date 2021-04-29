/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.ObjectiveFunctionResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;

import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final BranchResult branchResult;
    private final SensitivityStatus sensitivityStatus;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       BranchResult branchResult,
                                       SensitivityStatus sensitivityStatus) {
        this.objectiveFunction = objectiveFunction;
        this.branchResult = branchResult;
        this.sensitivityStatus = sensitivityStatus;
    }


    @Override
    public double getFunctionalCost() {
        return objectiveFunction.getFunctionalCost(branchResult, sensitivityStatus);
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(int number) {
        return objectiveFunction.getMostLimitingElements(branchResult, number);
    }

    @Override
    public double getVirtualCost() {
        return objectiveFunction.getVirtualCost(branchResult, sensitivityStatus);
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunction.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return objectiveFunction.getVirtualCost(branchResult, sensitivityStatus, virtualCostName);
    }

    @Override
    public List<BranchCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunction.getCostlyElements(branchResult, sensitivityStatus, virtualCostName, number);
    }
}
