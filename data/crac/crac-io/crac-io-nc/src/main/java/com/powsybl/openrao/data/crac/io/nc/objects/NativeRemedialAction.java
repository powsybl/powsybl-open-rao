/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.objects;

import com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracUtils;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at rte-france.com>}
 */
public interface NativeRemedialAction extends IdentifiedObjectWithOperator {
    String kind();

    boolean normalAvailable();

    String timeToImplement();

    boolean isManual();

    default Integer getTimeToImplementInSeconds() {
        if (timeToImplement() == null) {
            return null;
        }
        return NcCracUtils.convertDurationToSeconds(timeToImplement());
    }
}
