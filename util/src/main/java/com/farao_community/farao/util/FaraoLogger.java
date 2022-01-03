/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class FaraoLogger {
    public static final Logger BUSINESS_LOGS = LoggerFactory.getLogger("com.farao_community.farao.RaoBusinessLogs");
    public static final Logger BUSINESS_WARNS = LoggerFactory.getLogger("com.farao_community.farao.BusinessWarns");
    public static final Logger TECHNICAL_LOGS = LoggerFactory.getLogger("com.farao_community.farao.TechnicalLogs");

    private FaraoLogger() {
        // utility class
    }
}
