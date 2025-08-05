/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.ucte;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class HvdcRangeActionHelper {

    private HvdcRangeActionHelper() {
    }

    public static HvdcLine getHvdcLine(Network network, String networkElementId) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElementId);
        if (hvdcLine == null) {
            throw new OpenRaoException(String.format("HvdcLine %s does not exist in the current network.", networkElementId));
        }
        return hvdcLine;
    }

    public static double getCurrentSetpoint(Network network, String networkElementId) {
        if (getHvdcLine(network, networkElementId).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER) {
            return getHvdcLine(network, networkElementId).getActivePowerSetpoint();
        } else {
            return -getHvdcLine(network, networkElementId).getActivePowerSetpoint();
        }
    }
}
