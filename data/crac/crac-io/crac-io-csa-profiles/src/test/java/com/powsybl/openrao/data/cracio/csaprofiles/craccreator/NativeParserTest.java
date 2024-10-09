/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator;

import com.powsybl.openrao.data.cracio.csaprofiles.nc.NCObject;
import com.powsybl.openrao.data.cracio.csaprofiles.NativeParser;
import com.powsybl.triplestore.api.PropertyBag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NativeParserTest {
    public record MockClass(String mrid, Integer intValue, Boolean booleanValue, Double doubleValue, String stringValue) implements NCObject {
    }

    @Test
    void fromPropertyBag() throws Exception {
        PropertyBag data = new PropertyBag(List.of("mockClass", "intValue", "booleanValue", "doubleValue", "stringValue"), false);
        data.put("mockClass", "id");
        data.put("intValue", "0");
        data.put("doubleValue", "2.3");

        MockClass mockObject = NativeParser.fromPropertyBag(data, MockClass.class, Map.of("booleanValue", true, "stringValue", "Hello world!"));
        assertEquals(new MockClass("id", 0, true, 2.3, "Hello world!"), mockObject);
    }
}
