/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/***
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairImpl implements SwitchPair {
    private final NetworkElement switchToOpen;
    private final NetworkElement switchToClose;

    SwitchPairImpl(NetworkElement switchToOpen, NetworkElement switchToClose) {
        this.switchToOpen = switchToOpen;
        this.switchToClose = switchToClose;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        return !network.getSwitch(switchToOpen.getId()).isOpen() || network.getSwitch(switchToClose.getId()).isOpen();
    }

    @Override
    public boolean canBeApplied(Network network) {
        // It is only applicable if, initially, one switch was closed and the other was open.
        return network.getSwitch(switchToOpen.getId()).isOpen() != network.getSwitch(switchToClose.getId()).isOpen();
    }

    @Override
    public void apply(Network network) {
        new SwitchActionBuilder()
                .withId("idOpen")
                .withNetworkElementId(switchToOpen.getId())
                .withOpen(true)
            .build()
            .toModification()
            .apply(network);
        new SwitchActionBuilder()
                .withId("idClose")
                .withNetworkElementId(switchToClose.getId())
                .withOpen(false)
            .build()
            .toModification()
            .apply(network);
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Set.of(switchToOpen, switchToClose);
    }

    @Override
    public NetworkElement getSwitchToOpen() {
        return switchToOpen;
    }

    @Override
    public NetworkElement getSwitchToClose() {
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
        return this.switchToOpen.equals(oSwitchPair.getSwitchToOpen()) && this.switchToClose.equals(oSwitchPair.getSwitchToClose());
    }

    @Override
    public int hashCode() {
        return switchToOpen.hashCode() + 37 * switchToClose.hashCode();
    }
}
