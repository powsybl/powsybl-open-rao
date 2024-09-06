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
public record AssessedElement(String mrid, boolean inBaseCase, String name, String operator, String conductingEquipment, String operationalLimit, boolean isCombinableWithContingency, boolean isCombinableWithRemedialAction, boolean normalEnabled, String securedForRegion, String scannedForRegion, double flowReliabilityMargin, String overlappingZone) implements IdentifiedObjectWithOperator {
    public static AssessedElement fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElement(
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IN_BASE_CASE)),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NAME),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATOR),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_CONDUCTING_EQUIPMENT),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_OPERATIONAL_LIMIT),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_CONTINGENCY, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_IS_COMBINABLE_WITH_REMEDIAL_ACTION, "false")),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_NORMAL_ENABLED, "true")),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SECURED_FOR_REGION),
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_SCANNED_FOR_REGION),
            propertyBag.get(CsaProfileConstants.REQUEST_FLOW_RELIABILITY_MARGIN) == null ? 0d : Double.parseDouble(propertyBag.get(CsaProfileConstants.REQUEST_FLOW_RELIABILITY_MARGIN)),
            propertyBag.get(CsaProfileConstants.OVERLAPPING_ZONE));
    }
}
