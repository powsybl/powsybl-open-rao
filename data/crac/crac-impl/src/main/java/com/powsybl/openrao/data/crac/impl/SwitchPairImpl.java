/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.modification.NetworkModificationList;
import com.powsybl.openrao.data.crac.api.networkaction.SwitchPair;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairImpl implements SwitchPair {
    private final String id;
    private final String switchToOpen;
    private final String switchToClose;

    SwitchPairImpl(String id, String switchToOpen, String switchToClose) {
        this.id = id;
        this.switchToOpen = switchToOpen;
        this.switchToClose = switchToClose;
    }

    @Override
    public NetworkModification toModification() {
        return new NetworkModificationList(
            new SwitchActionBuilder()
                .withId(id + "_open")
                .withNetworkElementId(switchToOpen)
                .withOpen(true)
                .build()
                .toModification(),
            new SwitchActionBuilder()
                .withId(id + "_close")
                .withNetworkElementId(switchToClose)
                .withOpen(false)
                .build()
                .toModification()
        );
    }

    @Override
    public String getSwitchToOpenId() {
        return switchToOpen;
    }

    @Override
    public String getSwitchToCloseId() {
        return switchToClose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwitchPair oSwitchPair = (SwitchPairImpl) o;
        return this.getSwitchToOpenId().equals(oSwitchPair.getSwitchToOpenId()) && this.getSwitchToCloseId().equals(oSwitchPair.getSwitchToCloseId());
    }

    @Override
    public int hashCode() {
        return switchToOpen.hashCode() + 37 * switchToClose.hashCode();
    }

    @Override
    public String getType() {
        return "SWITCH_PAIR";
    }

    @Override
    public String getId() {
        return id;
    }
}
