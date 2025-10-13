/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.networkaction;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface SwitchPairAdder {

    SwitchPairAdder withSwitchToOpen(String networkElementId);

    SwitchPairAdder withSwitchToClose(String networkElementId);

    SwitchPairAdder withSwitchToOpen(String networkElementId, String networkElementName);

    SwitchPairAdder withSwitchToClose(String networkElementId, String networkElementName);

    NetworkActionAdder add();
}
