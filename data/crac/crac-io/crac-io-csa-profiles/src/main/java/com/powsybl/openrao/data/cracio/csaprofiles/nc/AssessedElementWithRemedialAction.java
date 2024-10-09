package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

public record AssessedElementWithRemedialAction(String mrid, String assessedElement, String remedialAction, String combinationConstraintKind, boolean normalEnabled) implements Association {
    public static AssessedElementWithRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElementWithRemedialAction(
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT),
            propertyBag.getId(CsaProfileConstants.REQUEST_REMEDIAL_ACTION),
            propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true"))
        );
    }
}
