/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class TestUtils {
    private TestUtils() {
        // should not be used
    }

    public static JsonParser parserFrom(String json) throws IOException {
        JsonParser p = new JsonFactory().createParser(json);
        p.nextToken();
        return p;
    }
}
