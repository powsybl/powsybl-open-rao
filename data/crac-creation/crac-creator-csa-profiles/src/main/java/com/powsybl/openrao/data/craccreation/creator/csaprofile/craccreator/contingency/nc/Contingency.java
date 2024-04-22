package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.contingency.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.triplestore.api.PropertyBag;

public record Contingency(String identifier, boolean normalMustStudy, String name, String operator) {

    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name, operator).orElse(identifier);
    }

    public static Contingency fromPropertyBag(PropertyBag propertyBag) {
        return new Contingency(propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCY), Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_NORMAL_MUST_STUDY)), propertyBag.getId(CsaProfileConstants.REQUEST_CONTINGENCIES_NAME), propertyBag.get(CsaProfileConstants.REQUEST_CONTINGENCIES_EQUIPMENT_OPERATOR));
    }
}
