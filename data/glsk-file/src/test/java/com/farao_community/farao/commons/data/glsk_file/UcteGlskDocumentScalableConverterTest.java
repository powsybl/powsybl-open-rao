/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

import com.farao_community.farao.commons.chronology.DataChronology;
import com.farao_community.farao.commons.data.glsk_file.actors.UcteGlskDocumentScalableConverter;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public class UcteGlskDocumentScalableConverterTest {

    private static final String UCTETEST = "/20170322_1844_SN3_FR2_GLSK_test.xml";
    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20160729_0000_GSK_allday_test.xml";
    private static final String COUNTRYTESTAMENI = "/GLSK_CWE_30_06_2019.xml";

    private Network testNetwork;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronology() throws ParserConfigurationException, SAXException, IOException {
        Map<String, DataChronology<Scalable>> mapGlskDocScalable = new UcteGlskDocumentScalableConverter().convertUcteGlskDocumentToScalableDataChronologyFromFileName(UCTETEST, testNetwork);
        assertTrue(!mapGlskDocScalable.isEmpty());

        for (String country : mapGlskDocScalable.keySet()) {
            DataChronology<Scalable> dataChronology = mapGlskDocScalable.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent());
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }
    }

    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronologyFromFilePathString() throws ParserConfigurationException, SAXException, IOException {
        String filepathstring = getClass().getResource("/20170322_1844_SN3_FR2_GLSK_test.xml").getPath();
        assertTrue(!new UcteGlskDocumentScalableConverter().convertUcteGlskDocumentToScalableDataChronologyFromFilePathString(filepathstring, testNetwork).isEmpty());
    }

    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronologyFromFilePath() throws ParserConfigurationException, SAXException, IOException {
        Path pathtest = Paths.get(getClass().getResource("/20170322_1844_SN3_FR2_GLSK_test.xml").getPath());
        assertTrue(!new UcteGlskDocumentScalableConverter().convertUcteGlskDocumentToScalableDataChronologyFromFilePath(pathtest, testNetwork).isEmpty());
    }

    //Ameni todo verifier tests
    @Test
    public void testConvertUcteGlskDocumentToScalableDataChronologyCountryFull() throws ParserConfigurationException, SAXException, IOException {
        List<Country> generators = testNetwork.getGeneratorStream().map(g -> g.getTerminal().getVoltageLevel().getSubstation().getCountry().get()).collect(Collectors.toList());
        List<String> generatorsBe = testNetwork.getGeneratorStream().filter(g -> (g.getTerminal().getVoltageLevel().getSubstation().getCountry().get()).equals(Country.BE)).map(g -> g.getId()).collect(Collectors.toList());
        Map<String, DataChronology<Scalable>> mapGlskDocScalable = new UcteGlskDocumentScalableConverter().convertUcteGlskDocumentToScalableDataChronologyFromFileName(COUNTRYFULL, testNetwork);
        assertTrue(!mapGlskDocScalable.isEmpty());

        for (String country : mapGlskDocScalable.keySet()) {
            DataChronology<Scalable> dataChronology = mapGlskDocScalable.get(country);
            assertTrue(dataChronology.getDataForInstant(Instant.parse("2016-07-28T22:00:00Z")).isPresent()); //TODO Attention ici on ajoute :00
            assertFalse(dataChronology.getDataForInstant(Instant.parse("2018-08-26T21:00:00Z")).isPresent());
        }
    }
}
