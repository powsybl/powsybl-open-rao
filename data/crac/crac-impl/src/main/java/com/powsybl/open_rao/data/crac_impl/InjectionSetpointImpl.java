/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.network_action.InjectionSetpoint;
import com.powsybl.open_rao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.Set;

/**
 * Injection setpoint remedial action: set a load or generator at a given value.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class InjectionSetpointImpl implements InjectionSetpoint {
    private static final double EPSILON = 0.1;

    private final NetworkElement networkElement;
    private final double setpoint;
    private final Unit unit;

    InjectionSetpointImpl(NetworkElement networkElement, double setpoint, Unit unit) {
        this.networkElement = networkElement;
        this.setpoint = setpoint;
        this.unit = unit;
    }

    @Override
    public double getSetpoint() {
        return setpoint;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public boolean hasImpactOnNetwork(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof Generator generator) {
            return Math.abs(generator.getTargetP() - setpoint) >= EPSILON;
        } else if (identifiable instanceof Load load) {
            return Math.abs(load.getP0() - setpoint) >= EPSILON;
        } else if (identifiable instanceof DanglingLine danglingLine) {
            return Math.abs(danglingLine.getP0() - setpoint) >= EPSILON;
        } else if (identifiable instanceof ShuntCompensator shuntCompensator) {
            return Math.abs(shuntCompensator.getSectionCount() - setpoint) >= EPSILON;
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads, dangling lines or shunt compensator");
        }
    }

    @Override
    public boolean canBeApplied(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof ShuntCompensator) {
            ShuntCompensator shuntCompensator = (ShuntCompensator) identifiable;
            return shuntCompensator.getMaximumSectionCount() < setpoint;
        }
        return true;
    }

    @Override
    public void apply(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof Generator generator) {
            generator.setTargetP(setpoint);
        } else if (identifiable instanceof Load load) {
            load.setP0(setpoint);
        } else if (identifiable instanceof DanglingLine danglingLine) {
            danglingLine.setP0(setpoint);
        } else if (identifiable instanceof ShuntCompensator shuntCompensator) {
            shuntCompensator.setSectionCount((int) setpoint);
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads, dangling lines or shunt compensators");
        }
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InjectionSetpointImpl oInjectionSetPoint = (InjectionSetpointImpl) o;
        return oInjectionSetPoint.getNetworkElement().equals(this.networkElement)
            && oInjectionSetPoint.getSetpoint() == this.setpoint;
    }

    @Override
    public int hashCode() {
        return networkElement.hashCode() + 37 * Double.valueOf(setpoint).hashCode();
    }
}
