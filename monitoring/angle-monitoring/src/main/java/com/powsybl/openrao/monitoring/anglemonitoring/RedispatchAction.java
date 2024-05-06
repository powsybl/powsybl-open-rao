/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.network.Network;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface RedispatchAction {
    /**
     * Scales power to redispatch (positive for generation, negative for load) on network.
     */
    void apply(Network network, double powerToRedispatch);
}
