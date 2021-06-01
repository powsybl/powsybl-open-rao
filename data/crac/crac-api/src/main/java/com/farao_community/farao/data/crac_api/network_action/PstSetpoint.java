/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api.network_action;

/***
 * A PST setpoint is an Elementary Action which consists in changing
 * the tap of a given PST.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface PstSetpoint extends ElementaryAction {

    /**
     * Get the new setpoint (tap) that will be applied on the network element of the action
     */
    int getSetpoint();
}
