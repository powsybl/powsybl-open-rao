/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearProblemResult {

    private final LinearProblem linearProblem;

    public LinearProblemResult(LinearProblem linearProblem) {
        this.linearProblem = linearProblem;
    }

    public double getSetpointOnState(RangeAction rangeAction, State state) {
        return linearProblem.getRangeActionSetpointVariable(rangeAction, state).solutionValue();
    }
}
