/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.NetworkElement;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class HvdcRangeActionUtils {

    private HvdcRangeActionUtils() {
    }

    public static HvdcLine getHvdcLine(Network network, NetworkElement networkElement) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        if (hvdcLine == null) {
            throw new OpenRaoException(String.format("HvdcLine %s does not exist in the current network.", networkElement.getId()));
        }
        return hvdcLine;
    }

    public static double getCurrentSetpoint(Network network, NetworkElement networkElement) {
        if (getHvdcLine(network, networkElement).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) {
            return getHvdcLine(network, networkElement).getActivePowerSetpoint();
        } else {
            return -getHvdcLine(network, networkElement).getActivePowerSetpoint();
        }
    }
}
