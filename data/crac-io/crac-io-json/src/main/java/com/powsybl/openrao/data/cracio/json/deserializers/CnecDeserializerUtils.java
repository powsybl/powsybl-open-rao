/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.FRM;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.RELIABILITY_MARGIN;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.getSubVersionNumber;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CnecDeserializerUtils {
    private CnecDeserializerUtils() {
    }

    public static void checkFrm(String version) {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, FRM));
        }
    }

    public static void checkReliabilityMargin(String version) {
        //"frm" renamed to "reliabilityMargin" in 1.4 and abandoned in 2.6
        int primaryVersionNumber = getPrimaryVersionNumber(version);
        int subVersionNumber = getSubVersionNumber(version);
        if (primaryVersionNumber <= 1 && subVersionNumber <= 3 || primaryVersionNumber >= 2 && subVersionNumber >= 6) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, RELIABILITY_MARGIN));
        }
    }
}
