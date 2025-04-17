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
public record RemedialActionScheme(String mrid, String kind, boolean normalArmed, String schemeRemedialAction) implements NCObject {
    public static RemedialActionScheme fromPropertyBag(PropertyBag propertyBag) {
        return new RemedialActionScheme(
            propertyBag.getId(NcConstants.REMEDIAL_ACTION_SCHEME),
            propertyBag.get(NcConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(NcConstants.NORMAL_ARMED)),
            propertyBag.getId(NcConstants.SCHEME_REMEDIAL_ACTION)
        );
    }
}
