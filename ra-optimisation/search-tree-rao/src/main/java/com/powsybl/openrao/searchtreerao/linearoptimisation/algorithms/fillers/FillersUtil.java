/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class FillersUtil {
    private FillersUtil() {
    }

    public static Set<State> getPreviousStates(State refState, OptimizationPerimeter optimizationContext) {
        return optimizationContext.getRangeActionsPerState().keySet().stream()
                .filter(s -> s.getContingency().equals(refState.getContingency()) || s.getContingency().isEmpty())
                .filter(s -> s.getInstant().comesBefore(refState.getInstant()) || s.getInstant().equals(refState.getInstant()))
                .collect(Collectors.toSet());
    }
}
