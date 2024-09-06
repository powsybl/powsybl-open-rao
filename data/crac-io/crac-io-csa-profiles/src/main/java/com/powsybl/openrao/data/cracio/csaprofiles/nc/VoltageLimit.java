/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record VoltageLimit(String mrid, double value, String equipment, String limitType, boolean isInfiniteDuration) implements NCObject {
    public static VoltageLimit fromPropertyBag(PropertyBag propertyBag) {
        return new VoltageLimit(
            propertyBag.getId(CsaProfileConstants.VOLTAGE_LIMIT),
            Double.parseDouble(propertyBag.get(CsaProfileConstants.VALUE)),
            propertyBag.getId(CsaProfileConstants.EQUIPMENT),
            propertyBag.get(CsaProfileConstants.LIMIT_TYPE),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.IS_INFINITE_DURATION, "true"))
        );
    }
}
