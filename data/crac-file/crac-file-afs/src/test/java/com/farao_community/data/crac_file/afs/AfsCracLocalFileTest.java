/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.data.crac_file.afs;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class AfsCracLocalFileTest {

    @Test
    public void createLocalFile() throws IOException {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));

        assertTrue(afsCracLocalFile.readBinaryData("cracData").isPresent());

        assertFalse(afsCracLocalFile.getDescription().isEmpty());

        assertEquals("cracFile", afsCracLocalFile.getPseudoClass());

        assertNotNull(afsCracLocalFile.getGenericMetadata());
        assertTrue(IOUtils.contentEquals(getClass().getResourceAsStream("/cracFileExampleValid.json"), afsCracLocalFile.readBinaryData("cracData").get()));
    }

    @Test (expected = AssertionError.class)
    public void dataExistsThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.dataExists("");
    }

    @Test (expected = AssertionError.class)
    public void getDataNamesThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getDataNames();
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesNamesThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getTimeSeriesNames();
    }

    @Test (expected = AssertionError.class)
    public void timeSeriesExistsThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.timeSeriesExists("");
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesMetadataThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getTimeSeriesMetadata(Mockito.mock(Set.class));
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesDataVersionsThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getTimeSeriesDataVersions();
    }

    @Test (expected = AssertionError.class)
    public void getTimeSeriesDataVersionsSThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getTimeSeriesDataVersions("");
    }

    @Test (expected = AssertionError.class)
    public void getDoubleTimeSeriesDataThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getDoubleTimeSeriesData(Mockito.mock(Set.class), 0);
    }

    @Test (expected = AssertionError.class)
    public void getStringTimeSeriesDataThrows() {
        AfsCracLocalFile afsCracLocalFile = new AfsCracLocalFile(Paths.get(getClass().getResource("/cracFileExampleValid.json").getPath()));
        afsCracLocalFile.getStringTimeSeriesData(Mockito.mock(Set.class), 0);
    }
}