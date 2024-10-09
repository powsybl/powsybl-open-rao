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
public record ContingencyWithRemedialAction(String mrid, String contingency, String remedialAction, String combinationConstraintKind, boolean normalEnabled) implements Association {
    public static ContingencyWithRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new ContingencyWithRemedialAction(
            propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY_WITH_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY),
            propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION),
            propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true"))
        );
    }
}
