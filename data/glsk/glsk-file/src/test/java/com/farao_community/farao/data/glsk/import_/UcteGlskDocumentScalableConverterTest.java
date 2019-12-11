/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.data.glsk.import_.actors.UcteGlskDocumentScalableConverter;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentScalableConverterTest {

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
        return getClass().getResource(resource).getPath();
    }

    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronologyFromFilePathString() throws ParserConfigurationException, SAXException, IOException {
        Map<String, DataChronology<Scalable>> mapGlskDocScalable = UcteGlskDocumentScalableConverter.convert(getResourceAsPathString(UCTETEST), testNetwork);
        assertFalse(mapGlskDocScalable.isEmpty());

        for (String country : mapGlskDocScalable.keySet()) {
            DataChronology<Scalable> dataChronology = mapGlskDocScalable.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }

        DataChronology<Scalable> dataChronolog = mapGlskDocScalable.get("10YFR-RTE------C");
        Scalable scalable =  dataChronolog.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).get();
        assertEquals("FFR1AA1 _generator", scalable.filterInjections(testNetwork).get(0).getId());
        assertEquals("FFR1AA1 _load", scalable.filterInjections(testNetwork).get(1).getId());
        assertEquals("FFR2AA1 _load", scalable.filterInjections(testNetwork).get(2).getId());
    }

    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronologyFromFilePath() throws ParserConfigurationException, SAXException, IOException {
        Map<String, DataChronology<Scalable>> mapGlskDocScalable = UcteGlskDocumentScalableConverter.convert(getResourceAsPath(UCTETEST), testNetwork);
        assertFalse(mapGlskDocScalable.isEmpty());

        for (String country : mapGlskDocScalable.keySet()) {
            DataChronology<Scalable> dataChronology = mapGlskDocScalable.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }

        DataChronology<Scalable> dataChronolog = mapGlskDocScalable.get("10YFR-RTE------C");
        Scalable scalable =  dataChronolog.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).get();
        assertEquals("FFR1AA1 _generator", scalable.filterInjections(testNetwork).get(0).getId());
        assertEquals("FFR1AA1 _load", scalable.filterInjections(testNetwork).get(1).getId());
        assertEquals("FFR2AA1 _load", scalable.filterInjections(testNetwork).get(2).getId());
    }

}
