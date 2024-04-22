/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record Contingency(String identifier, boolean normalMustStudy, String name, String operator) {

    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name, operator).orElse(identifier);
    }

    public static Contingency fromPropertyBag(PropertyBag propertyBag) {
        return new Contingency(propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY), Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY)), propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME), propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR));
    }
}
