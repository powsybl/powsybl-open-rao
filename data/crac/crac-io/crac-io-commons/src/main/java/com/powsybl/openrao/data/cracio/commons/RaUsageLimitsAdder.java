/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public final class RaUsageLimitsAdder {

    private RaUsageLimitsAdder() {
        // should not be used
    }

    public static void addRaUsageLimits(Crac crac, CracCreationParameters parameters) {
        parameters.getRaUsageLimitsPerInstant().forEach((instantName, raUsageLimits)
            -> crac.newRaUsageLimits(instantName)
            .withMaxRa(raUsageLimits.getMaxRa())
            .withMaxTso(raUsageLimits.getMaxTso())
            .withMaxRaPerTso(raUsageLimits.getMaxRaPerTso())
            .withMaxPstPerTso(raUsageLimits.getMaxPstPerTso())
            .withMaxTopoPerTso(raUsageLimits.getMaxTopoPerTso())
            .withMaxElementaryActionPerTso(raUsageLimits.getMaxElementaryActionsPerTso())
            .add());
    }
}
