/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cse.parameters;

import java.util.List;

/**
 * A class that maps BuBarChange remedial actions to switches that were added to the network
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BusBarChangeSwitches {
    private final String remedialActionId;
    private final List<String> switchesToOpen;
    private final List<String> switchesToClose;

    public BusBarChangeSwitches(String remedialActionId, List<String> switchesToOpen, List<String> switchesToClose) {
        this.remedialActionId = remedialActionId;
        this.switchesToOpen = switchesToOpen;
        this.switchesToClose = switchesToClose;
    }

    public String getRemedialActionId() {
        return remedialActionId;
    }

    public List<String> getSwitchesToOpen() {
        return switchesToOpen;
    }

    public List<String> getSwitchesToClose() {
        return switchesToClose;
    }
}
