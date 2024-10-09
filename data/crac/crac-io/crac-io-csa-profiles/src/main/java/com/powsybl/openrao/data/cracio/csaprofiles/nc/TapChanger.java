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
public record TapChanger(String mrid, String powerTransformer) implements NCObject {
    public static TapChanger fromPropertyBag(PropertyBag propertyBag) {
        return new TapChanger(propertyBag.getId(CsaProfileConstants.TAP_CHANGER), propertyBag.getId(CsaProfileConstants.POWER_TRANSFORMER));
    }
}
