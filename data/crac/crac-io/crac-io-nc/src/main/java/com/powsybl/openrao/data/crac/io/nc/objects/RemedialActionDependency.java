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
public record RemedialActionDependency(String mrid, String kind, String remedialAction, String dependingRemedialActionGroup, boolean normalEnabled) implements NCObject {
    public static RemedialActionDependency fromPropertyBag(PropertyBag propertyBag) {
        return new RemedialActionDependency(
            propertyBag.getId(NcConstants.REQUEST_REMEDIAL_ACTION_DEPENDENCY),
            propertyBag.get(NcConstants.KIND),
            propertyBag.getId(NcConstants.REQUEST_REMEDIAL_ACTION),
            propertyBag.getId(NcConstants.DEPENDING_REMEDIAL_ACTION_GROUP),
            Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.NORMAL_ENABLED, "true"))
        );
    }
}
