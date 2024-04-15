/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.SwitchActionBuilder;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.SwitchPair;
import com.powsybl.iidm.modification.NetworkModificationList;

/***
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairImpl implements SwitchPair {
    private final String id;
    private final NetworkElement switchToOpen;
    private final NetworkElement switchToClose;

    SwitchPairImpl(String id, NetworkElement switchToOpen, NetworkElement switchToClose) {
        this.id = id;
        this.switchToOpen = switchToOpen;
        this.switchToClose = switchToClose;
    }

    @Override
    public NetworkModification toModification() {
        return new NetworkModificationList(
            new SwitchActionBuilder()
                    .withId("idOpen")
                    .withNetworkElementId(switchToOpen.getId())
                    .withOpen(true)
                .build()
                .toModification(),
            new SwitchActionBuilder()
                .withId("idClose")
                .withNetworkElementId(switchToClose.getId())
                .withOpen(false)
            .build()
            .toModification()
        );
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

    @Override
    public String getType() {
        return "SWITCH_PAIR";
    }

    @Override
    public String getId() {
        return id;
    }
}
