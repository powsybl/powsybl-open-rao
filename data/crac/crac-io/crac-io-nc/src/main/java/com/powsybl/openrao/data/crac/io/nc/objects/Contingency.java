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
public record Contingency(String mrid, boolean normalMustStudy, String name, String operator) implements IdentifiedObjectWithOperator {
    public static Contingency fromPropertyBag(PropertyBag propertyBag) {
        return new Contingency(
            propertyBag.getId(NcConstants.REQUEST_CONTINGENCY),
            Boolean.parseBoolean(propertyBag.get(NcConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY)),
            propertyBag.getId(NcConstants.REQUEST_CONTINGENCIES_NAME),
            propertyBag.get(NcConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR)
        );
    }
}
