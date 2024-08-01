/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.nc;

import com.powsybl.openrao.data.cracio.csaprofiles.craccreator.CsaProfileCracUtils;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface RemedialAction extends IdentifiedObjectWithOperator {

    String mrid();

    String name();

    String operator();

    String kind();

    boolean normalAvailable();

    String timeToImplement();

    default Integer getTimeToImplementInSeconds() {
        if (timeToImplement() == null) {
            return null;
        }
        return CsaProfileCracUtils.convertDurationToSeconds(timeToImplement());
    }
}
