package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record GridStateAlterationRemedialAction(String mrid, String name, String remedialActionSystemOperator, String kind, Boolean normalAvailable, String timeToImplement) implements NCObject {
    public Integer getTimeToImplementInSeconds() {
        if (timeToImplement == null) {
            return null;
        }
        return CsaProfileCracUtils.convertDurationToSeconds(timeToImplement);
    }

    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name, remedialActionSystemOperator).orElse(mrid);
    }
}
