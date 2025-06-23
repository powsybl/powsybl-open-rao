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
public record TapPositionAction(String mrid, String tapChangerId, String propertyReference, boolean normalEnabled, String gridStateAlterationRemedialAction, String gridStateAlterationCollection) implements GridStateAlteration {
    public static TapPositionAction fromPropertyBag(PropertyBag propertyBag) {
        return new TapPositionAction(
            propertyBag.getId(NcConstants.TAP_POSITION_ACTION),
            propertyBag.getId(NcConstants.TAP_CHANGER_ID),
            propertyBag.get(NcConstants.GRID_ALTERATION_PROPERTY_REFERENCE),
            Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.NORMAL_ENABLED, "true")),
            propertyBag.getId(NcConstants.REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.getId(NcConstants.GRID_STATE_ALTERATION_COLLECTION)
        );
    }
}
