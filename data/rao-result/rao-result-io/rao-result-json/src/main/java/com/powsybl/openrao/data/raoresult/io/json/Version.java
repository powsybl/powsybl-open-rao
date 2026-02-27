/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public record Version(int major, int minor) {
    public static Version parse(String string) {
        Pattern versionPattern = Pattern.compile("^([1-9]\\d*)\\.(\\d+)$");
        Matcher versionMatcher = versionPattern.matcher(string);
        if (versionMatcher.find()) {
            return new Version(Integer.parseInt(versionMatcher.group(1)), Integer.parseInt(versionMatcher.group(2)));
        }
        throw new IllegalArgumentException("Invalid pattern for a version string.");
    }
}
