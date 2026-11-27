/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.triplestore.api.PropertyBag;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public record PowerBidScheduleTimePoint(String mrid, OffsetDateTime atTime, String powerBidSchedule, double p, double price) implements NCObject {

    public static PowerBidScheduleTimePoint fromPropertyBag(PropertyBag propertyBag) {
        return new PowerBidScheduleTimePoint(
            propertyBag.getId(NcConstants.POWER_BID_SCHEDULE_TIME_POINT),
            propertyBag.get(NcConstants.P) == null ? null : OffsetDateTime.parse(propertyBag.get(NcConstants.AT_TIME), DateTimeFormatter.ISO_DATE_TIME),
            propertyBag.getId(NcConstants.POWER_BID_SCHEDULE),
            propertyBag.get(NcConstants.P) == null ? null : Double.parseDouble(propertyBag.get(NcConstants.P)),
            propertyBag.get(NcConstants.P) == null ? null : Double.parseDouble(propertyBag.get(NcConstants.PRICE))
        );
    }
}
