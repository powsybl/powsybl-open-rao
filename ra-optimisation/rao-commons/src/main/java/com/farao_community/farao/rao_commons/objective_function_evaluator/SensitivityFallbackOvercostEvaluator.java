/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SensitivityFallbackOvercostEvaluator implements CostEvaluator {

    private double fallBackOvercost;

    public SensitivityFallbackOvercostEvaluator(double overcost) {
        this.fallBackOvercost = overcost;
    }

    public double computeCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {

        switch (sensitivityAndLoopflowResults.getSystematicSensitivityResult().getStatus()) {
            case SUCCESS:
                return 0.;
            case FALLBACK:
                return fallBackOvercost;
            case FAILURE:
            default:
                throw new FaraoException("Cannot evaluate cost as the sensitivity computation failed");
        }
    }

    public Unit getUnit() {
        return null;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements) {
        throw new NotImplementedException("getMostLimitingElements() not implemented yet for fallback cost evaluators");
    }
}
