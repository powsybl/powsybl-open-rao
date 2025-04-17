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
public record SchemeRemedialAction(String mrid, String name, String operator, String kind, boolean normalAvailable, String timeToImplement) implements RemedialAction {
    public static SchemeRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new SchemeRemedialAction(
            propertyBag.getId(NcConstants.SCHEME_REMEDIAL_ACTION),
            propertyBag.get(NcConstants.REMEDIAL_ACTION_NAME),
            propertyBag.get(NcConstants.TSO),
            propertyBag.get(NcConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(NcConstants.NORMAL_AVAILABLE)),
            propertyBag.get(NcConstants.TIME_TO_IMPLEMENT)
        );
    }
}
