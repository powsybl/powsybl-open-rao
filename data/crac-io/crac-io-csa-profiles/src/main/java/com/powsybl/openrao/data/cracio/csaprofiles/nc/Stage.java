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
public record Stage(String mrid, String gridStateAlterationCollection, String remedialActionScheme) implements NCObject {
    public static Stage fromPropertyBag(PropertyBag propertyBag) {
        return new Stage(
            propertyBag.getId(CsaProfileConstants.STAGE),
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_COLLECTION),
            propertyBag.getId(CsaProfileConstants.REMEDIAL_ACTION_SCHEME)
        );
    }
}
