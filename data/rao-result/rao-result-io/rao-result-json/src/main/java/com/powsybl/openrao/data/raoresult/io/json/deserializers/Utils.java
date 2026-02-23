/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.getSubVersionNumber;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class Utils {
    private Utils() {
        // should not be instanciated
    }

    static void checkDeprecatedField(JsonParser jsonParser, String parentName, String jsonFileVersion, String lastSupportedVersion) throws IOException {
        checkDeprecatedField(jsonParser.currentName(), parentName, jsonFileVersion, lastSupportedVersion);
    }

    static void checkDeprecatedField(String fieldName, String parentName, String jsonFileVersion, String lastSupportedVersion) {
        if (getPrimaryVersionNumber(jsonFileVersion) > getPrimaryVersionNumber(lastSupportedVersion)
                || getPrimaryVersionNumber(jsonFileVersion) == getPrimaryVersionNumber(lastSupportedVersion) && getSubVersionNumber(jsonFileVersion) > getSubVersionNumber(lastSupportedVersion)) {
            throw new OpenRaoException(String.format("Cannot deserialize RaoResult: field %s in %s in not supported in file version %s (last supported in version %s)", fieldName, parentName, jsonFileVersion, lastSupportedVersion));
        }
    }
}
