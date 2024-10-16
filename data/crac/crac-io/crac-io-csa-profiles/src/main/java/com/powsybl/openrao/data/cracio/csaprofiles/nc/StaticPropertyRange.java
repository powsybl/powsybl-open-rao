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
public record StaticPropertyRange(String mrid, double normalValue, String valueKind, String direction, String gridStateAlteration, String propertyReference) implements NCObject {
    public static StaticPropertyRange fromPropertyBag(PropertyBag propertyBag) {
        return new StaticPropertyRange(
            propertyBag.getId(CsaProfileConstants.STATIC_PROPERTY_RANGE),
            Double.parseDouble(propertyBag.get(CsaProfileConstants.NORMAL_VALUE)),
            propertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_VALUE_KIND),
            propertyBag.get(CsaProfileConstants.STATIC_PROPERTY_RANGE_DIRECTION),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION),
            propertyBag.get(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE)
        );
    }
}
