/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.NetworkElement;

import java.util.Map;

/**
 * A range action interface specifying an action on one or several Injections
 *
 * Injections can be Generator and/or Loads (DanglingLine are not yet taken into
 * account in that object).
 *
 * The InjectionRangeAction can affect several Injections, the conversion between
 * the setpoint of the RangeAction and the value - in megawatt - of each injection
 * can be defined with distribution keys.
 *
 * For instance, this implementation of RangeAction can be used to represent actions
 * on HVDCs, when HVDCs are modelled in the network with 2 Generators. In that case,
 * the appropriate InjectionRangeAction to represent the HVDC would be an action on
 * the two Generators, with distribution keys of 1 and -1.
 *
 * The 'generator convention' is used to represent the injections. A positive injection
 * is a positive generation or a negative load, and a negative injection is a negative
 * generation or a positive load.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface InjectionRangeAction extends StandardRangeAction<InjectionRangeAction> {

    /**
     * Get the map of the NetworkElement (injections) on which the rangeAction applies,
     * alongside with the distribution key which define how each injection NetworkElement
     * is impacted by a change of setpoint of the RangeAction
     */
    Map<NetworkElement, Double> getInjectionDistributionKeys();
}
