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
public record StaticPropertyRange(String mrid, double normalValue, String valueKind, String direction, String gridStateAlteration, String propertyReference) implements NCObject {
    public static StaticPropertyRange fromPropertyBag(PropertyBag propertyBag) {
        return new StaticPropertyRange(
            propertyBag.getId(NcConstants.STATIC_PROPERTY_RANGE),
            Double.parseDouble(propertyBag.get(NcConstants.NORMAL_VALUE)),
            propertyBag.get(NcConstants.STATIC_PROPERTY_RANGE_VALUE_KIND),
            propertyBag.get(NcConstants.STATIC_PROPERTY_RANGE_DIRECTION),
            propertyBag.getId(NcConstants.GRID_STATE_ALTERATION),
            propertyBag.get(NcConstants.GRID_ALTERATION_PROPERTY_REFERENCE)
        );
    }
}
