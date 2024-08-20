/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.api;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface LinearOptimizationResult extends FlowResult, SensitivityResult, RangeActionActivationResult, ObjectiveFunctionResult {

    LinearProblemStatus getStatus();

    RangeActionActivationResult getRangeActionActivationResult();

    @Override
    default ComputationStatus getComputationStatus() {
        return getSensitivityStatus();
    }

    @Override
    default ComputationStatus getComputationStatus(State state) {
        return getSensitivityStatus(state);
    }
}
