/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.networkaction;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.NetworkElement;

/***
 * An injection setpoint is an Elementary Action which consists in changing
 * the value of a given injection in the network.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface InjectionSetpoint extends ElementaryAction {

    /**
     * Get the new setpoint that will be applied on the network element of the action
     */
    double getSetpoint();

    /**
     * Get the Network Element associated to the elementary action
     */
    NetworkElement getNetworkElement();

    /**
     * Get the unit of the injection setpoint
     */
    Unit getUnit();

    @Override
    default boolean isCompatibleWith(ElementaryAction otherElementaryAction) {
        if (otherElementaryAction instanceof InjectionSetpoint injectionSetpoint) {
            return !getNetworkElement().equals(injectionSetpoint.getNetworkElement()) || getSetpoint() == injectionSetpoint.getSetpoint() && getUnit() == injectionSetpoint.getUnit();
        }
        return true;
    }
}
