package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.triplestore.api.PropertyBag;

public record AssessedElementWithRemedialAction(String mrid, String assessedElement, String remedialAction, String combinationConstraintKind, boolean normalEnabled) implements Association {
    public static AssessedElementWithRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new AssessedElementWithRemedialAction(
            propertyBag.getId(NcConstants.REQUEST_ASSESSED_ELEMENT_WITH_REMEDIAL_ACTION),
            propertyBag.getId(NcConstants.REQUEST_ASSESSED_ELEMENT),
            propertyBag.getId(NcConstants.REQUEST_REMEDIAL_ACTION),
            propertyBag.get(NcConstants.COMBINATION_CONSTRAINT_KIND),
            Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.NORMAL_ENABLED, "true"))
        );
    }
}
