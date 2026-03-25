/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.io.utils.BufferSize;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CracImportTest {

    @Test
    void testImportFromInputStream() throws IOException, URISyntaxException {
        var json = Path.of(getClass().getClassLoader().getResource("crac.json").toURI()).toFile();
        assertEquals("crac", Crac.read(json, null).getId());

        var importer = new Importer() {

            @Override
            public String getFormat() {
                return "";
            }

            @Override
            public boolean exists(SafeFileReader inputFile) {
                return false;
            }

            @Override
            public CracCreationContext importData(SafeFileReader inputFile,
                CracCreationParameters cracCreationParameters, Network network) {
                var all = inputFile.withReadStream(is -> new String(is.readAllBytes()));
                Assertions.assertEquals("Example CRAC file", all);
                return null;
            }
        };

        var reader = SafeFileReader.create(json, BufferSize.MEDIUM);
        importer.importData(reader, CracCreationParameters.load(), null);

    }
}
