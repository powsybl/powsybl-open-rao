/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil.addHvdcLine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class CracAliasesCreatorTest {

    private final Network network = Network.read("case-for-aliases.uct", getClass().getResourceAsStream("/case-for-aliases.uct"));

    @ParameterizedTest
    @ValueSource(strings = {"deprecated-crac-for-aliases-1.json", "deprecated-crac-for-aliases-2.json", "deprecated-crac-for-aliases-3.json", "deprecated-crac-for-aliases-4.json"})
    void testDeprecatedCracExtensions(String fileName) throws IOException {
        // Extensions have been deprecated
        addHvdcLine(network);
        InputStream inputStream = getClass().getResourceAsStream("/" + fileName);
        CracCreationContext cracCreationContext = Crac.readWithContext(fileName, inputStream, network);
        assertFalse(cracCreationContext.isCreationSuccessful());
        assertEquals(List.of("[ERROR] Extensions are deprecated since CRAC version 1.7"), cracCreationContext.getCreationReport().getReport());
        // TODO : instead of failing import, now that we have CracCreationContext with Json CRAC, ignore extensions and log the issue
    }
}
