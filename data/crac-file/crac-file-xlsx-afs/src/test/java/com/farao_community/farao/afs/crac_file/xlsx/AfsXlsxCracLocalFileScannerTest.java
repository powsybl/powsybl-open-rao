/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
package com.farao_community.farao.afs.crac_file.xlsx;


import com.powsybl.afs.local.storage.LocalFile;
import com.powsybl.afs.local.storage.LocalFileScannerContext;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class AfsXlsxCracLocalFileScannerTest {
    @Test
    public void scanTest() {
        AfsXlsxCracLocalFileScanner fileScanner = new AfsXlsxCracLocalFileScanner();
        LocalFile afsCracLocalFile = fileScanner.scanFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()), Mockito.mock(LocalFileScannerContext.class));
        assertNotNull(afsCracLocalFile);
        assertEquals("xlsxCracFile", afsCracLocalFile.getPseudoClass());
        assertEquals("20170215_xlsx_crac_fr_v01_v2", afsCracLocalFile.getName());
    }

    @Test
    public void scanNonValidTest() {
        AfsXlsxCracLocalFileScanner fileScanner = new AfsXlsxCracLocalFileScanner();
        LocalFile afsCracLocalFile = fileScanner.scanFile(Paths.get(getClass().getResource("/20170215_xlsx_crac.nonValid").getPath()), Mockito.mock(LocalFileScannerContext.class));
        assertNull(afsCracLocalFile);
    }
}
