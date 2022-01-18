/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons.logs;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class FaraoLoggerProvider {
    public static final FaraoLogger BUSINESS_LOGS = new RaoBusinessLogs();
    public static final FaraoLogger BUSINESS_WARNS = new RaoBusinessWarns();
    public static final FaraoLogger TECHNICAL_LOGS = new TechnicalLogs();

    private FaraoLoggerProvider() {
        // utility class
    }
}
