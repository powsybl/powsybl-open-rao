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
public record GridStateAlterationCollection(String mrid) implements NCObject {
    public static GridStateAlterationCollection fromPropertyBag(PropertyBag propertyBag) {
        return new GridStateAlterationCollection(propertyBag.getId(NcConstants.GRID_STATE_ALTERATION_COLLECTION));
    }
}
