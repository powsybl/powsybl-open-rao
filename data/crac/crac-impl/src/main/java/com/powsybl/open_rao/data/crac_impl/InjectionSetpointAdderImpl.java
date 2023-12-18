/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.NetworkElement;
import com.powsybl.open_rao.data.crac_api.network_action.InjectionSetpoint;
import com.powsybl.open_rao.data.crac_api.network_action.InjectionSetpointAdder;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkActionAdder;

import static com.powsybl.open_rao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionSetpointAdderImpl implements InjectionSetpointAdder {

    private NetworkActionAdderImpl ownerAdder;
    private String networkElementId;
    private String networkElementName;
    private Double setpoint;
    private Unit unit;

    InjectionSetpointAdderImpl(NetworkActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
    }

    @Override
    public InjectionSetpointAdder withNetworkElement(String networkElementId) {
        this.networkElementId = networkElementId;
        return this;
    }

    @Override
    public InjectionSetpointAdder withNetworkElement(String networkElementId, String networkElementName) {
        this.networkElementId = networkElementId;
        this.networkElementName = networkElementName;
        return this;
    }

    @Override
    public InjectionSetpointAdder withSetpoint(double setpoint) {
        this.setpoint = setpoint;
        return this;
    }

    @Override
    public InjectionSetpointAdder withUnit(Unit unit) {
        this.unit = unit;
        return this;
    }

    @Override
    public NetworkActionAdder add() {
        assertAttributeNotNull(networkElementId, "InjectionSetPoint", "network element", "withNetworkElement()");
        assertAttributeNotNull(setpoint, "InjectionSetPoint", "setpoint", "withSetPoint()");
        assertAttributeNotNull(unit, "InjectionSetPoint", "unit", "withUnit()");
        if (unit == Unit.SECTION_COUNT && (setpoint < 0 || Math.abs(setpoint - Math.floor(setpoint)) > 1e-6)) {
            throw new OpenRaoException("With a SECTION_COUNT unit, setpoint should be a positive integer");
        }

        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        InjectionSetpoint injectionSetpoint = new InjectionSetpointImpl(networkElement, setpoint, unit);
        ownerAdder.addElementaryAction(injectionSetpoint);
        return ownerAdder;
    }
}
