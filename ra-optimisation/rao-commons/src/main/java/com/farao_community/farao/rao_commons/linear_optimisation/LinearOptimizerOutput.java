/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;

import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearOptimizerOutput {
    private final LinearProblem.SolveStatus solveStatus;
    private final Map<RangeAction, Double> rangeActionSetpoints;
    private final Map<PstRangeAction, Integer> pstRangeActionTaps;

    public LinearOptimizerOutput(LinearProblem.SolveStatus solveStatus, Map<RangeAction, Double> rangeActionSetpoints, Map<PstRangeAction, Integer> pstRangeActionTaps) {
        this.solveStatus = solveStatus;
        this.rangeActionSetpoints = rangeActionSetpoints;
        this.pstRangeActionTaps = pstRangeActionTaps;
    }

    public Map<RangeAction, Double> getRangeActionSetpoints() {
        return rangeActionSetpoints;
    }

    public Map<PstRangeAction, Integer> getPstRangeActionTaps() {
        return pstRangeActionTaps;
    }

    public LinearProblem.SolveStatus getSolveStatus() {
        return solveStatus;
    }
}
