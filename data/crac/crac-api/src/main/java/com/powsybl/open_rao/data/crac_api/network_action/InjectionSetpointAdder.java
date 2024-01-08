/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_api.network_action;

import com.powsybl.open_rao.commons.Unit;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface InjectionSetpointAdder {

    InjectionSetpointAdder withNetworkElement(String networkElementId);

    InjectionSetpointAdder withNetworkElement(String networkElementId, String networkElementName);

    InjectionSetpointAdder withSetpoint(double setPoint);

    InjectionSetpointAdder withUnit(Unit unit);

    NetworkActionAdder add();

}
