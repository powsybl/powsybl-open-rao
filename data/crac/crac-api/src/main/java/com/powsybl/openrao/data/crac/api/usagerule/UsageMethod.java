/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import java.util.Set;

/**
 * Usage method of a remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum UsageMethod {
    AVAILABLE,
    FORCED,
    UNAVAILABLE,
    UNDEFINED;

    public static UsageMethod getStrongestUsageMethod(Set<UsageMethod> usageMethods) {
        if (usageMethods.contains(UNAVAILABLE)) {
            return UNAVAILABLE;
        } else if (usageMethods.contains(FORCED)) {
            return FORCED;
        } else if (usageMethods.contains(AVAILABLE)) {
            return AVAILABLE;
        } else if (usageMethods.contains(UNDEFINED)) {
            return UNDEFINED;
        }
        return UNAVAILABLE;
    }
}
