/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record Version(int major, int minor) implements Comparable<Version> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^([1-9]\\d*)\\.(\\d+)$");

    @Override
    public String toString() {
        return major + "." + minor;
    }

    @Override
    public int compareTo(Version other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        return Integer.compare(minor, other.minor);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version(int major1, int minor1)) {
            return major == major1 && minor == minor1;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return major * 100 + minor;
    }

    public static Version parse(String version) {
        if (version == null) {
            throw new OpenRaoException("Version cannot be null");
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new OpenRaoException("Invalid version format: " + version);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return new Version(major, minor);
    }
}
