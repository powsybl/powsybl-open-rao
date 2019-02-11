/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.afs.crac_file.xlsx;

import com.farao_community.data.crac_file.afs.AfsCracLocalFile;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.Assert.*;

public class AfsCracLocalFileTest {

    @Test
    public void createLocalFile() throws IOException {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));

        assertTrue(afsCracLocalFile.readBinaryData("cracData").isPresent());

        assertFalse(afsCracLocalFile.getDescription().isEmpty());

        assertEquals("cracFile", afsCracLocalFile.getPseudoClass());

        assertNotNull(afsCracLocalFile.getGenericMetadata());
        assertTrue(IOUtils.contentEquals(getClass().getResourceAsStream("/20170215_xlsx_crac_fr_v01_v2.3.xlsx"), afsCracLocalFile.readBinaryData("cracData").get()));
    }

    @Test (expected = AssertionError.class)
    public void dataExistsThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.dataExists("");
    }

    @Test (expected = AssertionError.class)
    public void getDataNamesThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getDataNames();
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesNamesThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getTimeSeriesNames();
    }

    @Test (expected = AssertionError.class)
    public void timeSeriesExistsThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.timeSeriesExists("");
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesMetadataThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getTimeSeriesMetadata(Mockito.mock(Set.class));
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesDataVersionsThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getTimeSeriesDataVersions();
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesDataVersionsSThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getTimeSeriesDataVersions("");
    }

    @Test (expected = AssertionError.class)
    public void getDoubleTimeSeriesDataThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getDoubleTimeSeriesData(Mockito.mock(Set.class), 0);
    }

    @Test (expected = AssertionError.class)
    public void getStringTimeSeriesDataThrows() {
        AfsXlsxCracLocalFile afsXlsxCracLocalFile = new AfsXlsxCracLocalFile(Paths.get(getClass().getResource("/20170215_xlsx_crac_fr_v01_v2.3.xlsx").getPath()));
        afsXlsxCracLocalFile.getStringTimeSeriesData(Mockito.mock(Set.class), 0);
    }
}
