/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.RangeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResult<T extends RangeAction<T>> implements Result<T> {
    protected Map<String, Double> setPointPerStates;

    public RangeActionResult(Set<String> states) {
        setPointPerStates = new HashMap<>();
        states.forEach(state -> setPointPerStates.put(state, Double.NaN));
    }

    public Set<String> getStates() {
        return setPointPerStates.keySet();
    }

    public double getSetPoint(String state) {
        return setPointPerStates.getOrDefault(state, Double.NaN);
    }

    public void setSetPoint(String state, double setPoint) {
        setPointPerStates.put(state, setPoint);
    }

    public boolean isActivated(String state) {
        return !setPointPerStates.getOrDefault(state, Double.NaN).isNaN();
    }

}
