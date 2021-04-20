/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;

import java.util.List;

/**
 * Represents an objective function value evaluator, divided into functional and virtual parts
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ObjectiveFunctionEvaluator extends CostEvaluator {
    List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements);

    /**
     * Computes the functional part of the objective function
     */
    double computeFunctionalCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults);

    /**
     * Computes the virtual part of the objective function
     */
    double computeVirtualCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults);
}
