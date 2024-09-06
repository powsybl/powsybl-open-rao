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
public record AssessedElement(String mrid, boolean inBaseCase, String name, String operator, String conductingEquipment, String operationalLimit, boolean isCombinableWithContingency, boolean isCombinableWithRemedialAction, boolean normalEnabled, String securedForRegion, String scannedForRegion, double flowReliabilityMargin) implements IdentifiedObjectWithOperator {
    public static AssessedElement fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElement(
            propertyBag.getId(CsaProfileConstants.ASSESSED_ELEMENT),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.IN_BASE_CASE)),
            propertyBag.get(CsaProfileConstants.NAME),
            propertyBag.get(CsaProfileConstants.OPERATOR),
            propertyBag.getId(CsaProfileConstants.CONDUCTING_EQUIPMENT),
            propertyBag.getId(CsaProfileConstants.OPERATIONAL_LIMIT),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.IS_COMBINABLE_WITH_CONTINGENCY, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.IS_COMBINABLE_WITH_REMEDIAL_ACTION, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true")),
            propertyBag.get(CsaProfileConstants.SECURED_FOR_REGION),
            propertyBag.get(CsaProfileConstants.SCANNED_FOR_REGION),
            propertyBag.get(CsaProfileConstants.FLOW_RELIABILITY_MARGIN) == null ? 0d : Double.parseDouble(propertyBag.get(CsaProfileConstants.FLOW_RELIABILITY_MARGIN))
        );
    }
}
