/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.ucte;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

/**
 * FlowBased Glsk Values Provider Test for Ucte format
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteGlskValueProviderTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testProvideOkUcteGlsk() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<LinearGlsk> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
            .getZonalGlsks(network, instant);
        assertEquals(3, ucteGlskProvider.getData("10YFR-RTE------C").getGLSKs().size());
        assertEquals(0.3, ucteGlskProvider.getData("10YFR-RTE------C").getGLSKs().get("FFR1AA1 _generator"), EPSILON);
    }

    @Test
    public void testProvideUcteGlskEmptyInstant() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2020-07-29T10:00:00Z");

        ZonalData<LinearGlsk> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
            .getZonalGlsks(network, instant);

        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    public void testProvideUcteGlskUnknownCountry() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<LinearGlsk> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
            .getZonalGlsks(network, instant);

        assertNull(ucteGlskProvider.getData("unknowncountry"));
    }

    @Test
    public void testProvideUcteGlskWithWrongFormat() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        ZonalData<LinearGlsk> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(network, instant);
        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    public void testMultiGskSeries() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestMultiGskSeries.xml"));
        ZonalData<Scalable> ucteScalableProvider = ucteGlskDocument.getZonalScalable(network, instant);
        assertEquals(2, ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).size());
        assertTrue(ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).contains(network.getGenerator("FFR1AA1 _generator")));
        assertTrue(ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).contains(network.getGenerator("FFR2AA1 _generator")));

        ZonalData<LinearGlsk> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertEquals(2, ucteGlskProvider.getData("10YFR-RTE------C").getGLSKs().size());
        assertEquals(0.5, ucteGlskProvider.getData("10YFR-RTE------C").getGLSKs().get("FFR1AA1 _generator"), EPSILON);
        assertEquals(0.5, ucteGlskProvider.getData("10YFR-RTE------C").getGLSKs().get("FFR2AA1 _generator"), EPSILON);
    }

    @Test
    public void checkConversionOfMultiGskSeriesToScalable() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        AbstractGlskPoint multiGlskSeries = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/ThreeGskSeries.xml")).getGlskPoints("10YFR-RTE------C").get(0);
        Scalable scalable = GlskPointScalableConverter.convert(network, multiGlskSeries);
        double generationBeforeScale = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        assertEquals(24500.0, generationBeforeScale, 0.1);
        assertEquals(2000.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.1);
        assertEquals(2000.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.1);

        scalable.scale(network, 1000.0);

        double generationAfterScale = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        assertEquals(25500.0, generationAfterScale, 0.1);
        assertEquals(2700.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.1);
        assertEquals(2300.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.1);
    }
}
