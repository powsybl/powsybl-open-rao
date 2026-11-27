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
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public record PowerBidSchedule(String mrid, String powerRemedialAction, String direction, double activationCost) implements NCObject {
    public static PowerBidSchedule fromPropertyBag(PropertyBag propertyBag) {
        return new PowerBidSchedule(
            propertyBag.getId(NcConstants.POWER_BID_SCHEDULE),
            propertyBag.getId(NcConstants.POWER_REMEDIAL_ACTION),
            propertyBag.get(NcConstants.DIRECTION),
            propertyBag.get(NcConstants.ACTIVATION_COST) == null ? null : Double.parseDouble(propertyBag.get(NcConstants.ACTIVATION_COST))
        );
    }
}
