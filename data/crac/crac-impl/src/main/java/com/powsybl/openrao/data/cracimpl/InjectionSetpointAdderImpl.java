/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpointAdder;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionSetpointAdderImpl implements InjectionSetpointAdder {

    public static final String INJECTION_SET_POINT = "InjectionSetPoint";
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
        assertAttributeNotNull(networkElementId, INJECTION_SET_POINT, "network element", "withNetworkElement()");
        assertAttributeNotNull(setpoint, INJECTION_SET_POINT, "setpoint", "withSetPoint()");
        assertAttributeNotNull(unit, INJECTION_SET_POINT, "unit", "withUnit()");
        if (unit == Unit.SECTION_COUNT && (setpoint < 0 || Math.abs(setpoint - Math.floor(setpoint)) > 1e-6)) {
            throw new OpenRaoException("With a SECTION_COUNT unit, setpoint should be a positive integer");
        }

        NetworkElement networkElement = this.ownerAdder.getCrac().addNetworkElement(networkElementId, networkElementName);
        InjectionSetpoint injectionSetpoint = new InjectionSetpointImpl(networkElement, setpoint, unit);
        ownerAdder.addElementaryAction(injectionSetpoint, networkElement);
        return ownerAdder;
    }
}
