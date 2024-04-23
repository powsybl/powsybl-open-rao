package com.powsybl.openrao.data.craccreation.creator.csaprofile.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

public record AssessedElementWithContingency(String identifier, String assessedElement, String contingency, String combinationConstraintKind, boolean normalEnabled) {
    public static AssessedElementWithContingency fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElementWithContingency(
            propertyBag.get(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT_WITH_CONTINGENCY),
            propertyBag.getId(CsaProfileConstants.REQUEST_ASSESSED_ELEMENT),
            propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY),
            propertyBag.get(CsaProfileConstants.COMBINATION_CONSTRAINT_KIND),
            Boolean.parseBoolean(propertyBag.getOrDefault(CsaProfileConstants.NORMAL_ENABLED, "true"))
        );
    }
}
