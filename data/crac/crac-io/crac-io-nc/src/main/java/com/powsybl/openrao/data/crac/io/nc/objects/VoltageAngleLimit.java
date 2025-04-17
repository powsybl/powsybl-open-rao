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
public record VoltageAngleLimit(String mrid, double normalValue, String terminal1, String terminal2, String direction, Boolean isFlowToRefTerminal) implements NCObject {
    public static VoltageAngleLimit fromPropertyBag(PropertyBag propertyBag) {
        return new VoltageAngleLimit(
            propertyBag.getId(NcConstants.REQUEST_VOLTAGE_ANGLE_LIMIT),
            Double.parseDouble(propertyBag.get(NcConstants.REQUEST_VOLTAGE_ANGLE_LIMIT_NORMAL_VALUE)),
            // TODO: add constants
            propertyBag.getId("terminal1"),
            propertyBag.getId("terminal2"),
            propertyBag.get(NcConstants.REQUEST_OPERATIONAL_LIMIT_DIRECTION),
            propertyBag.get(NcConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL) == null ? null : Boolean.parseBoolean(propertyBag.get(NcConstants.REQUEST_IS_FLOW_TO_REF_TERMINAL))
        );
    }
}
