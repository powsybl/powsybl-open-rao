/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.cse;

import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskDocumentImporterTest {
    private static final double EPSILON = 1e-3;

    @Test
    public void checkCseGlskDocumentImporterCorrectlyImportManualGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        List<AbstractGlskPoint> list = cseGlskDocument.getGlskPoints("FR_MANUAL");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyConvertManualGskBlocks() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable manualScalable = glskDocument.getZonalScalable(network).getData("FR_MANUAL");

        assertNotNull(manualScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        manualScalable.scale(network, 1000.);
        assertEquals(2700., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyImportReserveGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        List<AbstractGlskPoint> list = cseGlskDocument.getGlskPoints("FR_RESERVE");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyConvertReserveGskBlocks() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable reserveScalable = glskDocument.getZonalScalable(network).getData("FR_RESERVE");

        assertNotNull(reserveScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);

        reserveScalable.scale(network, -900.);
        assertEquals(1400., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1700., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyImportPropGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        List<AbstractGlskPoint> list = cseGlskDocument.getGlskPoints("FR_PROPGSK");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(3, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyConvertPropGskBlocks() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGskScalable = glskDocument.getZonalScalable(network).getData("FR_PROPGSK");

        assertNotNull(propGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        propGskScalable.scale(network, 700.);
        assertEquals(2200., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2200., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyImportPropGlskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        List<AbstractGlskPoint> list = cseGlskDocument.getGlskPoints("FR_PROPGLSK");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(2, list.get(0).getGlskShiftKeys().size());
        assertEquals(3, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(1).getRegisteredResourceArrayList().size());
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyConvertPropGlskBlocks() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGlskScalable = glskDocument.getZonalScalable(network).getData("FR_PROPGLSK");

        assertNotNull(propGlskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000., network.getLoad("FFR1AA1 _load").getP0(), EPSILON);
        assertEquals(3500., network.getLoad("FFR2AA1 _load").getP0(), EPSILON);

        propGlskScalable.scale(network, 1000.);
        assertEquals(2200., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2200., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
        assertEquals(933.3334, network.getLoad("FFR1AA1 _load").getP0(), EPSILON);
        assertEquals(3266.6667, network.getLoad("FFR2AA1 _load").getP0(), EPSILON);
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyImportMeritOrderGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        List<AbstractGlskPoint> list = cseGlskDocument.getGlskPoints("FR_MERITORDER");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(4, list.get(0).getGlskShiftKeys().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(1).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(2).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(3).getRegisteredResourceArrayList().size());
    }

    @Test
    public void checkCseGlskDocumentImporterCorrectlyConvertMeritOrderGskBlocks() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGskScalable = glskDocument.getZonalScalable(network).getData("FR_MERITORDER");

        assertNotNull(propGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        propGskScalable.scale(network, 5000.);
        assertEquals(5000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(4000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

}
