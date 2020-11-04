/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.data.glsk.import_.actors.UcteGlskDocumentLinearGlskConverter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentLinearGlskConverterTest {

    private static final String UCTETEST = "/20170322_1844_SN3_FR2_GLSK_test.xml";

    private Network testNetwork;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
    }

    private Path getResourceAsPath(String resource) {
        return Paths.get(getResourceAsPathString(resource));
    }

    private String getResourceAsPathString(String resource) {
        return new File(getClass().getResource(resource).getFile()).getAbsolutePath();
    }

    @Test
    public void testConvertUcteGlskDocumentToLinearGlskDataChronologyFromFilePathString() {
        UcteGlskDocumentLinearGlskConverter converter = new UcteGlskDocumentLinearGlskConverter();
        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = converter.convert(getResourceAsPathString(UCTETEST), testNetwork);
        assertFalse(mapGlskDocLinearGlsk.isEmpty());

        for (String country : mapGlskDocLinearGlsk.keySet()) {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }
    }

    @Test
    public void testConvertUcteGlskDocumentToLinearGlskDataChronologyFromFilePath() {
        UcteGlskDocumentLinearGlskConverter converter = new UcteGlskDocumentLinearGlskConverter();
        Map<String, DataChronology<LinearGlsk>> mapGlskDocLinearGlsk = converter.convert(getResourceAsPath(UCTETEST), testNetwork);
        assertFalse(mapGlskDocLinearGlsk.isEmpty());

        for (String country : mapGlskDocLinearGlsk.keySet()) {
            DataChronology<LinearGlsk> dataChronology = mapGlskDocLinearGlsk.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }
    }
}
