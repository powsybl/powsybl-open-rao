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
public record RemedialActionDependency(String mrid, String kind, String remedialAction, String dependingRemedialActionGroup, boolean normalEnabled) implements NCObject {
    public static RemedialActionDependency fromPropertyBag(PropertyBag propertyBag) {
        return new RemedialActionDependency(
            propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY),
            propertyBag.get(CsaProfileConstants.KIND),
            propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.DEPENDING_REMEDIAL_ACTION_GROUP),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true"))
        );
    }
}
