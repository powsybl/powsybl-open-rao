/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomizedString.class);

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
            LOGGER.error("There should at least be one try to generate randomized string.");
            throw new IllegalArgumentException("There should at least be one try to generate randomized string.");
        }
        for (int tryNum = 0; tryNum < maxTry; tryNum++) {
            String randomizedString = prefix + UUID.randomUUID();
            if (!invalidStrings.contains(randomizedString)) {
                return randomizedString;
            }
        }
        LOGGER.error("Failed to create a randomized string with prefix '{}' in {} {}.", prefix, maxTry, maxTry > 1 ? "tries" : "try");
        throw new FaraoException(String.format("Failed to create a randomized string with prefix '%s' in %d %s.", prefix, maxTry, maxTry > 1 ? "tries" : "try"));
    }
}
