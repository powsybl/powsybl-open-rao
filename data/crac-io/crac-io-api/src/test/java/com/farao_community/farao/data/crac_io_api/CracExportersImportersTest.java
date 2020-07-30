/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracExportersImportersTest {

    String cracFile = "/empty.txt";

    @Test
    public void testExport() {
        Crac crac = Mockito.mock(Crac.class);
        CracExporters.exportCrac(crac, "", Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath()));
    }

    @Test
    public void testImport() {
        Crac crac = CracImporters.importCrac(Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath()));
        Assert.assertNotNull(crac);
    }

    @Test
    public void testImportWithInstant() {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(0);
        Crac crac = CracImporters.importCrac(Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath()), instant);
        Assert.assertNotNull(crac);
    }
}
