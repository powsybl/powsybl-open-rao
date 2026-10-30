/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator;

import com.powsybl.openrao.data.crac.io.commons.OpenRaoImportException;
import com.powsybl.openrao.data.crac.io.commons.api.ImportStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.NETWORK;
import static com.powsybl.openrao.data.crac.io.nc.craccreator.NcCracCreationTestUtil.cracCreationDefaultParametersWithSweCsaExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class NcCracImporterTest {

    @Test
    void ncp25PackageIsRejected() {
        try (InputStream in = getClass().getResourceAsStream("/profiles/ncp25/NCP25.zip")) {
            OpenRaoImportException exception = assertThrows(OpenRaoImportException.class,
                () -> new NcCracImporter().importData(in, cracCreationDefaultParametersWithSweCsaExtension(), NETWORK));
            assertEquals(ImportStatus.NOT_YET_HANDLED_BY_OPEN_RAO, exception.getImportStatus());
            assertTrue(exception.getMessage().contains("NCP version 2.5 is not supported"));
            assertTrue(exception.getMessage().contains("CO-v2.5.rdf"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
