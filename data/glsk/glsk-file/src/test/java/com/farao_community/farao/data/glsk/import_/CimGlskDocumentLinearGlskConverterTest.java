/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.data.glsk.import_.actors.CimGlskDocumentLinearGlskConverter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class CimGlskDocumentLinearGlskConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskDocumentLinearGlskConverterTest.class);

    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";

    private Network testNetwork;

    private Path getResourceAsPath(String resource) {
        return Paths.get(getResourceAsPathString(resource));
    }

    private String getResourceAsPathString(String resource) {
        return new File(getClass().getResource(resource).getFile()).getAbsolutePath();
    }

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void testConvertGlskDocumentToLinearGlskDataChronologyFromFilePathString() {
        CimGlskDocumentLinearGlskConverter converter = new CimGlskDocumentLinearGlskConverter();
        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = converter.convert(getResourceAsPath(GLSKB42COUNTRYIIDM), testNetwork);
        assertFalse(mapGlskDocLinearGlsk.isEmpty());
        for (String country : mapGlskDocLinearGlsk.keySet()) {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2018-08-29T21:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
            assertEquals(country, dataChronology.getDataForInstant(Instant.parse("2018-08-29T21:00:00Z")).get().getName());
        }
    }

    @Test
    public void testConvertGlskDocumentToLinearGlskDataChronologyFromFilePath() {
        Path pathtest = Paths.get("src/test/resources/GlskB42CountryIIDM.xml");
        CimGlskDocumentLinearGlskConverter converter = new CimGlskDocumentLinearGlskConverter();
        assertFalse(converter.convert(pathtest, testNetwork).isEmpty());
    }
}
