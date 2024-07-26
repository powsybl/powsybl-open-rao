/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.networkaction;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface GeneratorActionAdder {

    GeneratorActionAdder withNetworkElement(String networkElementId);

    GeneratorActionAdder withNetworkElement(String networkElementId, String networkElementName);

    GeneratorActionAdder withActivePowerValue(double setPoint);

    NetworkActionAdder add();

}
