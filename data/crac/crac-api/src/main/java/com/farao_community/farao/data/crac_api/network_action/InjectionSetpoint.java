/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;

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
}
