/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.parameters;

import com.powsybl.openrao.data.cracapi.parameters.AbstractAlignedRaCracCreationParameters;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CseCracCreationParameters extends AbstractAlignedRaCracCreationParameters {

    private Map<String, BusBarChangeSwitches> busBarChangeSwitchesMap = new HashMap<>();

    @Override
    public String getName() {
        return "CseCracCreatorParameters";
    }

    public void setBusBarChangeSwitchesSet(Set<BusBarChangeSwitches> busBarChangeSwitchesList) {
        this.busBarChangeSwitchesMap = new HashMap<>();
        busBarChangeSwitchesList.forEach(busBarChangeSwitches -> busBarChangeSwitchesMap.put(busBarChangeSwitches.getRemedialActionId(), busBarChangeSwitches));
    }

    public Set<BusBarChangeSwitches> getBusBarChangeSwitchesSet() {
        return new HashSet<>(busBarChangeSwitchesMap.values());
    }

    public BusBarChangeSwitches getBusBarChangeSwitches(String remedialActionId) {
        return busBarChangeSwitchesMap.get(remedialActionId);
    }
}
