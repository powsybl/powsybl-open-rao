package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.constants.CsaProfileConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record GridStateAlterationRemedialAction(String mrid, String name, String operator, String kind, boolean normalAvailable, String timeToImplement) implements RemedialAction {
    public static GridStateAlterationRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new GridStateAlterationRemedialAction(
            propertyBag.getId(CsaProfileConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME),
            propertyBag.get(CsaProfileConstants.TSO),
            propertyBag.get(CsaProfileConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE)),
            propertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT)
        );
    }
}
