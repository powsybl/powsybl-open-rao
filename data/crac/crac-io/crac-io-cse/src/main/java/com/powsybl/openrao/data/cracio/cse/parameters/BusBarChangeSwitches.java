/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.parameters;

import java.util.Set;

/**
 * A class that maps BuBarChange remedial actions to switches that were added to the network
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BusBarChangeSwitches {
    private final String remedialActionId;
    private final Set<SwitchPairId> switchPairs;

    public BusBarChangeSwitches(String remedialActionId, Set<SwitchPairId> switchPairs) {
        this.remedialActionId = remedialActionId;
        this.switchPairs = switchPairs;
    }

    public String getRemedialActionId() {
        return remedialActionId;
    }

    public Set<SwitchPairId> getSwitchPairs() {
        return switchPairs;
    }
}
