/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.*;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;

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
    public void apply(Network network) {
        Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
        if (identifiable instanceof Generator) {
            new GeneratorActionBuilder()
                    .withId("id")
                    .withGeneratorId(networkElement.getId())
                    .withActivePowerRelativeValue(false)
                    .withActivePowerValue(setpoint)
                .build()
                .toModification()
                .apply(network);
                // change of behavior: if P is not within bounds, min or max will be applied (without error), before the incorrect value was applied
        } else if (identifiable instanceof Load) {
            new LoadActionBuilder()
                    .withId("id")
                    .withLoadId(networkElement.getId())
                    .withRelativeValue(false)
                    .withActivePowerValue(setpoint)
                .build()
                .toModification()
                .apply(network);
        } else if (identifiable instanceof DanglingLine) {
            new DanglingLineActionBuilder()
                    .withId("id")
                    .withDanglingLineId(networkElement.getId())
                    .withRelativeValue(false)
                    .withActivePowerValue(setpoint)
                .build()
                .toModification()
                .apply(network);
        } else if (identifiable instanceof ShuntCompensator) {
            new ShuntCompensatorPositionActionBuilder()
                    .withId("id")
                    .withShuntCompensatorId(networkElement.getId())
                    .withSectionCount((int) setpoint)
                .build()
                .toModification()
                .apply(network);
        } else {
            throw new NotImplementedException("Injection setpoint only handled for generators, loads, dangling lines or shunt compensators");
        }
    }

    @Override
    public NetworkElement getNetworkElement() {
        return networkElement;
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
