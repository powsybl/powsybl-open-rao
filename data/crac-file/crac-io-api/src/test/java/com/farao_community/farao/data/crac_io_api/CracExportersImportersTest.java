/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracExportersImportersTest {

    @Test
    public void testExport() {
        Crac crac = Mockito.mock(Crac.class);
        CracExporters.exportCrac(crac, Mockito.anyString(), Paths.get(getClass().getResource("/empty.txt").getFile()));
    }

    @Test
    public void testImport() {
        CracImporters.importCrac(Paths.get(getClass().getResource("/empty.txt").getFile()));
    }
}
