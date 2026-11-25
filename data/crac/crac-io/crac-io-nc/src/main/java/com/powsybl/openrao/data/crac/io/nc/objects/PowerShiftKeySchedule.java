/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
public record PowerShiftKeySchedule(String mrid, String interpolationKind, String generatingUnit) implements NCObject {

    public static PowerShiftKeySchedule fromPropertyBag(PropertyBag propertyBag) {
        return new PowerShiftKeySchedule(
            propertyBag.getId(NcConstants.POWER_SHIFT_KEY_SCHEDULE),
            propertyBag.get(NcConstants.INTERPOLATION_KIND),
            propertyBag.get(NcConstants.GENERATING_UNIT)
        );
    }
}
