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
public record RemedialActionGroup(String mrid, String name) implements IdentifiedObject {
    public static RemedialActionGroup fromPropertyBag(PropertyBag propertyBag) {
        return new RemedialActionGroup(propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION_GROUP), propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME));
    }
}
