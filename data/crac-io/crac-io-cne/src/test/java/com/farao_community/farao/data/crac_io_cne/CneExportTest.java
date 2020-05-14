/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneExportTest {

    @Test
    public void testExport() {

        Crac crac = CracImporters.importCrac("small-crac-with-result-extensions.json", getClass().getResourceAsStream("/small-crac-with-result-extensions.json"));
        Crac crac1 =  Mockito.spy(crac);
        Mockito.doReturn(true).when(crac1).isSynchronized();

        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(crac1, "CNE", outputStream);
    }
}
