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
public record GlskBidActionDistribution(String mrid, String glskSchedule, String powerBidSchedule) implements NCObject {
    public static GlskBidActionDistribution fromPropertyBag(PropertyBag propertyBag) {
        return new GlskBidActionDistribution(
            propertyBag.getId(NcConstants.REQUEST_GLSK_BID_ACTION_DISTRIBUTION),
            propertyBag.getId(NcConstants.REQUEST_GLSK_SCHEDULE),
            propertyBag.getId(NcConstants.POWER_BID_SCHEDULE));
    }
}
