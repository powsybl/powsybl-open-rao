/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.cnec.Cnec;

/**
 * Enum representing the instants at which {@link Cnec} can be monitored and
 * {@link RemedialAction} applied.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum InstantKind {
    PREVENTIVE,
    OUTAGE,
    AUTO,
    CURATIVE
}
