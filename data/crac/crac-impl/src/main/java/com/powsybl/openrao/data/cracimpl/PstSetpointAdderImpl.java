/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.networkaction.PstSetpointAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstSetpointAdderImpl implements PstSetpointAdder {

    private NetworkActionAdderImpl ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private Integer setpoint;
    private static final String CLASS_NAME = "PstSetPoint";

    PstSetpointAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public PstSetpointAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public PstSetpointAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public PstSetpointAdder withSetpoint(int setPoint) {
        this.setpoint = setPoint;
        return this;
    }

    @Override
    public NetworkActionAdder add() {
        assertAttributeNotNull(networkElementId, CLASS_NAME, "network element", "withNetworkElement()");
        assertAttributeNotNull(setpoint, CLASS_NAME, "setpoint", "withSetPoint()");

        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        PstSetpointImpl pstSetpoint = new PstSetpointImpl(networkElement, setpoint);
        ownerAdder.addElementaryAction(pstSetpoint, networkElement);
        return ownerAdder;
    }
}
