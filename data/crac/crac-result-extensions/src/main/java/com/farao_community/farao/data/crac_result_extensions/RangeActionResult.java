/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.State;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.commons.extensions.Extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResult<I extends RangeAction<I>> extends AbstractExtension<I> implements Extension<I> {
    protected Map<String, Double> setPointPerStates;

    @JsonCreator
    public RangeActionResult(@JsonProperty("setPointPerStates") Map<String, Double> setPointPerStates) {
        this.setPointPerStates = new HashMap<>(setPointPerStates);
    }

    Map<String, Double> getSetPointPerStates() {
        return setPointPerStates;
    }

    void setSetPointPerStates(Map<String, Double> setPointPerStates) {
        this.setPointPerStates = setPointPerStates;
    }

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

    @Override
    public String getName() {
        return "RangeActionResult";
    }
}
