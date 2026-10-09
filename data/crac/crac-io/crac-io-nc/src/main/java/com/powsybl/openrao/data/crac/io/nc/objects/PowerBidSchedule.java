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
public record PowerBidSchedule(String mrid, String powerRemedialAction, double startupCost, double totalMinimumEnergy, double totalMaximumEnergy) implements NCObject {
    public static PowerBidSchedule fromPropertyBag(PropertyBag propertyBag) {
        return new PowerBidSchedule(
            propertyBag.getId(NcConstants.REQUEST_POWER_BID_SCHEDULE),
            propertyBag.getId(NcConstants.REQUEST_POWER_REMEDIAL_ACTION),
            propertyBag.get(NcConstants.REQUEST_STARTUP_COST) == null ? 0.0 : Double.parseDouble(propertyBag.get(NcConstants.REQUEST_STARTUP_COST)),
            propertyBag.get(NcConstants.REQUEST_TOTAL_MINIMUM_ENERGY) == null ? 0.0 : Double.parseDouble(propertyBag.get(NcConstants.REQUEST_TOTAL_MINIMUM_ENERGY)),
            propertyBag.get(NcConstants.REQUEST_TOTAL_MAXIMUM_ENERGY) == null ? 0.0 : Double.parseDouble(propertyBag.get(NcConstants.REQUEST_TOTAL_MAXIMUM_ENERGY)));
    }
}
