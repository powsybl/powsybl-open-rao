/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResult implements Result {

    protected Map<String, Double> setPointPerStates;

    public RangeActionResult(Map<String, Double> setPointPerStates) {
        this.setPointPerStates = setPointPerStates;
    }

    public RangeActionResult(Set<String> stateIds) {
        setPointPerStates = new HashMap<>();
        stateIds.forEach(state -> setPointPerStates.put(state, Double.NaN));
    }

    public double getSetPoint(String stateId) {
        return setPointPerStates.getOrDefault(stateId, Double.NaN);
    }

    public void setSetPoint(String stateId, double setPoint) {
        setPointPerStates.put(stateId, setPoint);
    }

    @Deprecated // this method doesn't work correctly, and should be deleted
    public boolean isActivated(String stateId) {
        return !setPointPerStates.getOrDefault(stateId, Double.NaN).isNaN();
    }

}
