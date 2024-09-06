package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

public record TapChanger(String mrid, String powerTransformer) implements NCObject {
    public static TapChanger fromPropertyBag(PropertyBag propertyBag) {
        return new TapChanger(propertyBag.getId(CsaProfileConstants.TAP_CHANGER), propertyBag.getId(CsaProfileConstants.POWER_TRANSFORMER));
    }
}
