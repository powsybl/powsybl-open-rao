/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class StateIdHelper {
    private StateIdHelper() {
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public static String getStateId(Contingency contingency, Instant instant, OffsetDateTime timestamp) {
        String idWithContingency = contingency == null ? instant.getId() : contingency.getId() + " - " + instant.getId();
        return timestamp == null ? idWithContingency : idWithContingency + " - " + timestamp.format(DATE_TIME_FORMATTER);
    }

    public static String getStateId(Instant instant, OffsetDateTime timestamp) {
        return getStateId(null, instant, timestamp);
    }
}
