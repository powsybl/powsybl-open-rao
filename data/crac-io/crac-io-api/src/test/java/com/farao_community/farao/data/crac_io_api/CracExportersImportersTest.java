/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertThrows;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracExportersImportersTest {

    private String cracFile = "/empty.txt";

    @Test
    public void testExport() {
        Crac crac = Mockito.mock(Crac.class);
        Path path = Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath());
        assertThrows(NotImplementedException.class, () -> CracExporters.exportCrac(crac, "Mock", path));
    }

    @Test
    public void testImport() {
        Crac crac = CracImporters.importCrac(Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath()));
        Assert.assertNotNull(crac);
    }

    @Test
    public void testImportWithInstant() {
        Crac crac = CracImporters.importCrac(Paths.get(new File(getClass().getResource(cracFile).getFile()).getAbsolutePath()));
        Assert.assertNotNull(crac);
    }
}
