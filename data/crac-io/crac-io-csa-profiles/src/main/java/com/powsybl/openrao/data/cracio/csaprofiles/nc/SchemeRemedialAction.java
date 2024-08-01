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
public record SchemeRemedialAction(String mrid, String name, String operator, String kind, boolean normalAvailable, String timeToImplement) implements RemedialAction {
    public static SchemeRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new SchemeRemedialAction(
            propertyBag.getId(CsaProfileConstants.SCHEME_REMEDIAL_ACTION),
            propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME),
            propertyBag.get(CsaProfileConstants.TSO),
            propertyBag.get(CsaProfileConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE)),
            propertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT)
        );
    }
}
