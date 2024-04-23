/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.cim;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record CurrentLimit(String identifier, double value, String terminal, String limitType, String direction, String acceptableDuration) {
    public static CurrentLimit fromPropertyBag(PropertyBag propertyBag) {
        return new CurrentLimit(
            propertyBag.getId(CsaProfileConstants.REQUEST_CURRENT_LIMIT),
            Double.parseDouble(propertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_VALUE)),
            propertyBag.getId(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TERMINAL),
            propertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_TYPE),
            propertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION),
            propertyBag.get(CsaProfileConstants.REQUEST_OPERATIONAL_LIMIT_ACCEPTABLE_DURATION)
        );
    }
}
