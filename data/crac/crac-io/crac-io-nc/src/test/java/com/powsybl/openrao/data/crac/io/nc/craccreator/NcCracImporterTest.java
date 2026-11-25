/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NcCracImporterTest {

    /**
     * NCP 2.5 packages are no longer rejected by the importer (see issue #2057): the zip is
     * digested and read as an NCP 2.5 package. The CRAC creation itself fails on the
     * missing timestamp of the default parameters, which is unrelated to the import.
     */
    @Test
    void ncp25PackageIsNoLongerRejected() {
        NcCracImporter importer = new NcCracImporter();
        CracCreationParameters parameters = cracCreationDefaultParametersWithSweCsaExtension();
        try (InputStream in = getClass().getResourceAsStream("/profiles/ncp25/NCP25.zip")) {
            CracCreationContext creationContext = importer.importData(in, parameters, NETWORK);
            assertNotNull(creationContext);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
