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
public record RemedialActionScheme(String mrid, String kind, boolean normalArmed, String schemeRemedialAction) implements NCObject {
    public static RemedialActionScheme fromPropertyBag(PropertyBag propertyBag) {
        return new RemedialActionScheme(
            propertyBag.getId(CsaProfileConstants.REMEDIAL_ACTION_SCHEME),
            propertyBag.get(CsaProfileConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.NORMAL_ARMED)),
            propertyBag.getId(CsaProfileConstants.SCHEME_REMEDIAL_ACTION)
        );
    }
}
