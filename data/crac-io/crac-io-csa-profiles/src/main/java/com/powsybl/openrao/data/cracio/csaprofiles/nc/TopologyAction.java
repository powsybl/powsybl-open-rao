/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record TopologyAction(String mrid, String switchId, String propertyReference, boolean normalEnabled, String gridStateAlterationRemedialAction, String gridStateAlterationCollection) implements GridStateAlteration {
    public static TopologyAction fromPropertyBag(PropertyBag propertyBag) {
        return new TopologyAction(
            propertyBag.getId(CsaProfileConstants.TOPOLOGY_ACTION),
            propertyBag.getId(CsaProfileConstants.SWITCH),
            propertyBag.get(CsaProfileConstants.PROPERTY_REFERENCE),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true")),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION)
        );
    }
}
