/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class RandomizedString {
    private static final String DEFAULT_PREFIX = "";
    private static final int DEFAULT_MAX_TRY = 2;

    private RandomizedString() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static String getRandomizedString() {
        return getRandomizedString(DEFAULT_PREFIX, Collections.emptyList(), DEFAULT_MAX_TRY);
    }

    public static String getRandomizedString(String prefix) {
        return getRandomizedString(prefix, Collections.emptyList(), DEFAULT_MAX_TRY);
    }

    public static String getRandomizedString(Collection<String> invalidStrings) {
        return getRandomizedString(DEFAULT_PREFIX, invalidStrings, DEFAULT_MAX_TRY);
    }

    public static String getRandomizedString(String prefix, Collection<String> invalidStrings) {
        return getRandomizedString(prefix, invalidStrings, DEFAULT_MAX_TRY);
    }

    public static String getRandomizedString(String prefix, Collection<String> invalidStrings, int maxTry) {
        Objects.requireNonNull(prefix);
        if (maxTry < 1) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("There should at least be one try to generate randomized string.");
            throw new IllegalArgumentException("There should at least be one try to generate randomized string.");
        }
        for (int tryNum = 0; tryNum < maxTry; tryNum++) {
            String randomizedString = prefix + UUID.randomUUID();
            if (!invalidStrings.contains(randomizedString)) {
                return randomizedString;
            }
        }
        OpenRaoLoggerProvider.TECHNICAL_LOGS.error("Failed to create a randomized string with prefix '{}' in {} {}.", prefix, maxTry, maxTry > 1 ? "tries" : "try");
        throw new OpenRaoException(String.format("Failed to create a randomized string with prefix '%s' in %d %s.", prefix, maxTry, maxTry > 1 ? "tries" : "try"));
    }
}
