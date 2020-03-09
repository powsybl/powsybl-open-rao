/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResult<I extends RangeAction<I>> extends AbstractExtension<I> {
    protected Map<State, Double> setPointPerStates;

    public RangeActionResult(Set<State> states) {
        setPointPerStates = new HashMap<>();
        states.forEach(state -> setPointPerStates.put(state, Double.NaN));
    }

    public double getSetPoint(State state) {
        return setPointPerStates.getOrDefault(state, Double.NaN);
    }

    public void setSetPoint(State state, double setPoint) {
        setPointPerStates.put(state, setPoint);
    }

    public boolean isActivated(State state) {
        return !setPointPerStates.getOrDefault(state, Double.NaN).isNaN();
    }

    @Override
    public String getName() {
        return "RangeActionResult";
    }
}
