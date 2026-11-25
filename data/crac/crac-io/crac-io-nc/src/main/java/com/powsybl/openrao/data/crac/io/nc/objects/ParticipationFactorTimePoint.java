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
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record ParticipationFactorTimePoint(String powerShiftKeySchedule, OffsetDateTime atTime,
                                           Double participationFactor) {
    public static ParticipationFactorTimePoint fromPropertyBag(PropertyBag propertyBag) {
        String atTime = propertyBag.get(NcConstants.AT_TIME);
        String participationFactor = propertyBag.get(NcConstants.PARTICIPATION_FACTOR);
        return new ParticipationFactorTimePoint(
            propertyBag.getId(NcConstants.POWER_SHIFT_KEY_SCHEDULE),
            atTime == null ? null : OffsetDateTime.parse(atTime, DateTimeFormatter.ISO_DATE_TIME),
            participationFactor == null ? null : Double.parseDouble(participationFactor)
        );
    }
}
