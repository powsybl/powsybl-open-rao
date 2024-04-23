/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.nc;

import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileConstants;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracUtils;
import com.powsybl.triplestore.api.PropertyBag;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record GridStateAlterationRemedialAction(String identifier, String name, String operator, String kind, boolean normalAvailable, String timeToImplement) {

    public String getUniqueName() {
        return CsaProfileCracUtils.createElementName(name, operator).orElse(identifier);
    }

    public Integer getTimeToImplementInSeconds() {
        if (timeToImplement == null) {
            return null;
        }
        return CsaProfileCracUtils.convertDurationToSeconds(timeToImplement);
    }

    public static GridStateAlterationRemedialAction fromPropertyBag(PropertyBag propertyBag) {
        return new GridStateAlterationRemedialAction(
            propertyBag.getId(CsaProfileConstants.REQUEST_GRID_STATE_ALTERATION_REMEDIAL_ACTION),
            propertyBag.get(CsaProfileConstants.REMEDIAL_ACTION_NAME),
            propertyBag.get(CsaProfileConstants.TSO),
            propertyBag.get(CsaProfileConstants.KIND),
            Boolean.parseBoolean(propertyBag.get(CsaProfileConstants.NORMAL_AVAILABLE)),
            propertyBag.get(CsaProfileConstants.TIME_TO_IMPLEMENT)
        );
    }
}
