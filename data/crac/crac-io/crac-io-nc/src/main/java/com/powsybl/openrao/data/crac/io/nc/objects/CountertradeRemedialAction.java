/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public record CountertradeRemedialAction(String mrid, String name, String operator, String description, String kind, boolean normalAvailable,
                                         String penaltyFactor, boolean isCrossBorderRelevant, boolean isManual,
                                         String impactThresholdMargin, Double maxEconomicPMargin, Double minEconomicPMargin,
                                         String timeToImplement, String region) implements IdentifiedObjectWithOperator {
    public static CountertradeRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new CountertradeRemedialAction(
                propertyBag.getId(NcConstants.COUNTERTRADE_REMEDIAL_ACTION),
                propertyBag.get(NcConstants.REMEDIAL_ACTION_NAME),
                propertyBag.get(NcConstants.TSO),
                propertyBag.get(NcConstants.DESCRIPTION),
                propertyBag.get(NcConstants.KIND),
                Boolean.parseBoolean(propertyBag.get(NcConstants.NORMAL_AVAILABLE)),
                propertyBag.get(NcConstants.PENALTY_FACTOR),
                Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.IS_CROSS_BORDER_RELEVANT, "true")),
                Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.IS_MANUAL, "true")),
                propertyBag.get(NcConstants.IMPACT_THRESHOLD_MARGIN),
                Double.parseDouble(propertyBag.get(NcConstants.MAX_ECONOMIC_P_MARGIN)),
                Double.parseDouble(propertyBag.get(NcConstants.MIN_ECONOMIC_P_MARGIN)),
                propertyBag.get(NcConstants.TIME_TO_IMPLEMENT),
                propertyBag.get(NcConstants.APPOINTED_TO_REGION)
        );
    }

    public Integer getTimeToImplementInSeconds() {
        if (timeToImplement() == null) {
            return null;
        }
        return NcCracUtils.convertDurationToSeconds(timeToImplement());
    }

}
