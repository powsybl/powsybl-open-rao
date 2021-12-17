/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.network_action.*;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairAdderImpl implements SwitchPairAdder {

    private NetworkActionAdderImpl ownerAdder;
    private String switchToOpenId;
    private String switchToOpenName;
    private String switchToCloseId;
    private String switchToCloseName;

    SwitchPairAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public SwitchPairAdder withSwitchToOpen(String networkElementId) {
        return withSwitchToOpen(networkElementId, networkElementId);
    }

    @Override
    public SwitchPairAdder withSwitchToClose(String networkElementId) {
        return withSwitchToClose(networkElementId, networkElementId);
    }

    @Override
    public SwitchPairAdder withSwitchToOpen(String networkElementId, String networkElementName) {
        this.switchToOpenId = networkElementId;
        this.switchToOpenName = networkElementName;
        return this;
    }

    @Override
    public SwitchPairAdder withSwitchToClose(String networkElementId, String networkElementName) {
        this.switchToCloseId = networkElementId;
        this.switchToCloseName = networkElementName;
        return this;
    }

    @Override
    public NetworkActionAdder add() {
        assertAttributeNotNull(switchToOpenId, "SwitchPair", "switch to open", "withSwitchToOpen()");
        assertAttributeNotNull(switchToCloseId, "SwitchPair", "switch to close", "withSwitchToClose()");
        if (switchToOpenId.equals(switchToCloseId)) {
            throw new FaraoException("A switch pair cannot be created with the same switch to open & close!");
        }
        NetworkElement switchToOpen = this.ownerAdder.getCrac().addNetworkElement(switchToOpenId, switchToOpenName);
        NetworkElement switchToClose = this.ownerAdder.getCrac().addNetworkElement(switchToCloseId, switchToCloseName);
        ownerAdder.addElementaryAction(new SwitchPairImpl(switchToOpen, switchToClose));
        return ownerAdder;
    }
}
