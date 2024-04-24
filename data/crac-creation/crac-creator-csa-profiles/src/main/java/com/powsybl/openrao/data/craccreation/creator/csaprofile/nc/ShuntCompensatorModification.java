/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record ShuntCompensatorModification(String identifier, String shuntCompensatorId, String propertyReference, boolean normalEnabled, String gridStateAlterationRemedialAction, String gridStateAlterationCollection) {
    public static ShuntCompensatorModification fromPropertyBag(PropertyBag propertyBag) {
        return new ShuntCompensatorModification(
            propertyBag.getId(CsaProfileConstants.SHUNT_COMPENSATOR_MODIFICATION),
            propertyBag.getId(CsaProfileConstants.SHUNT_COMPENSATOR_ID),
            propertyBag.get(CsaProfileConstants.GRID_ALTERATION_PROPERTY_REFERENCE),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true")),
            propertyBag.getId(CsaProfileConstants.REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION)
        );
    }
}
