package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;
import com.powsybl.openrao.data.crac.io.nc.craccreator.constants.NcConstants;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record GridStateAlterationRemedialAction(String mrid, String name, String operator, String kind, boolean normalAvailable, String timeToImplement, boolean isManual) implements IdentifiedObjectWithOperator {
    public static GridStateAlterationRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new GridStateAlterationRemedialAction(
            propertyBag.getId(NcConstants.GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.get(NcConstants.REMEDIAL_ACTION_NAME),
            propertyBag.get(NcConstants.TSO),
            propertyBag.get(NcConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(NcConstants.NORMAL_AVAILABLE)),
            propertyBag.get(NcConstants.TIME_TO_IMPLEMENT),
            Boolean.parseBoolean(propertyBag.getOrDefault(NcConstants.IS_MANUAL, "true"))
        );
    }

    public Integer getTimeToImplementInSeconds() {
        if (timeToImplement() == null) {
            return null;
        }
        return NcCracUtils.convertDurationToSeconds(timeToImplement());
    }
}
