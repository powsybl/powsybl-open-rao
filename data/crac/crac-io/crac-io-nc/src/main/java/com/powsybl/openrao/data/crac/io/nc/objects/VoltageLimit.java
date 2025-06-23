/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record VoltageLimit(String mrid, double value, String equipment, String limitType, boolean isInfiniteDuration) implements NCObject {
    public static VoltageLimit fromPropertyBag(PropertyBag propertyBag) {
        return new VoltageLimit(
            propertyBag.getId(NcConstants.REQUEST_VOLTAGE_LIMIT),
            Double.parseDouble(propertyBag.get(NcConstants.REQUEST_OPERATIONAL_LIMIT_VALUE)),
            propertyBag.getId(NcConstants.REQUEST_OPERATIONAL_LIMIT_EQUIPMENT),
            propertyBag.get(NcConstants.REQUEST_OPERATIONAL_LIMIT_TYPE),
            Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.REQUEST_VOLTAGE_LIMIT_IS_INFINITE_DURATION, "true"))
        );
    }
}
