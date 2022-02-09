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

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface CostEvaluator {

    String getName();

    /**
     * It evaluates the cost of RaoData containing a Network, a Crac and a SystematicSensitivityResult on
     * the current RaoData variant.
     *
     * @return Double value of the RaoData cost.
     * @param flowResult
     */
    double computeCost(FlowResult flowResult, ComputationStatus sensitivityStatus);

    Unit getUnit();

    /**
     * Gets the most costly elements, ordered from most to least costly. Elements
     * with a null cost are not in the list.
     *
     * @param flowResult: The results to use.
     * @param numberOfElements: The size of the list to be studied, so the number of costly elements to be retrieved.
     * @return The ordered list of the n first costly elements.
     */
    List<FlowCnec> getCostlyElements(FlowResult flowResult, int numberOfElements);
}
