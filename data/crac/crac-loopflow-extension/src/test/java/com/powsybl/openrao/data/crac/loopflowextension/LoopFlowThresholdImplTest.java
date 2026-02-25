/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.Math.sqrt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class LoopFlowThresholdImplTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    private Network network;
    private Crac crac;
    private FlowCnec cnec;
    private double iMax;
    private double nominalV;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        cnec = crac.getFlowCnec("cnec2basecase");

        iMax = 1500.0;
        nominalV = 380.0;
    }

    @Test
    void basicSetterAndGetterTest() {
        cnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.PERCENT_IMAX)
            .withValue(1.)
            .add();

        LoopFlowThreshold loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class);

        assertNotNull(loopFlowThreshold);
        assertEquals(1., loopFlowThreshold.getValue(), DOUBLE_TOLERANCE);
        assertEquals(Unit.PERCENT_IMAX, loopFlowThreshold.getUnit());
    }

    @Test
    void convertFromPercent() {
        cnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.PERCENT_IMAX)
            .withValue(0.5)
            .add();

        LoopFlowThreshold loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class);

        assertNotNull(loopFlowThreshold);
        assertEquals(0.5, loopFlowThreshold.getThreshold(Unit.PERCENT_IMAX), DOUBLE_TOLERANCE);
        assertEquals(0.5 * iMax, loopFlowThreshold.getThreshold(Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(0.5 * iMax * nominalV * sqrt(3) / 1000, loopFlowThreshold.getThreshold(Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void convertFromA() {
        cnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.AMPERE)
            .withValue(750)
            .add();

        LoopFlowThreshold loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class);

        assertNotNull(loopFlowThreshold);
        assertEquals(750 / iMax, loopFlowThreshold.getThreshold(Unit.PERCENT_IMAX), DOUBLE_TOLERANCE);
        assertEquals(750, loopFlowThreshold.getThreshold(Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(750 * nominalV * sqrt(3) / 1000, loopFlowThreshold.getThreshold(Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void convertFromMW() {
        cnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(1000)
            .add();

        LoopFlowThreshold loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class);

        assertNotNull(loopFlowThreshold);
        assertEquals(1000 * 1000 / (nominalV * sqrt(3) * iMax), loopFlowThreshold.getThreshold(Unit.PERCENT_IMAX), DOUBLE_TOLERANCE);
        assertEquals(1000 * 1000 / (nominalV * sqrt(3)), loopFlowThreshold.getThreshold(Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1000, loopFlowThreshold.getThreshold(Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void getThresholdWithFrm() {

        FlowCnec cnecWithFrm = crac.getFlowCnec("cnec2stateCurativeContingency2"); // contains frm of 95. MW

        cnecWithFrm.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(1000)
            .add();

        LoopFlowThreshold loopFlowThreshold = cnecWithFrm.getExtension(LoopFlowThreshold.class);
        assertEquals(905. * 1000 / (nominalV * sqrt(3) * iMax), loopFlowThreshold.getThresholdWithReliabilityMargin(Unit.PERCENT_IMAX), DOUBLE_TOLERANCE);
        assertEquals(905. * 1000 / (nominalV * sqrt(3)), loopFlowThreshold.getThresholdWithReliabilityMargin(Unit.AMPERE), DOUBLE_TOLERANCE);
        assertEquals(905., loopFlowThreshold.getThresholdWithReliabilityMargin(Unit.MEGAWATT), DOUBLE_TOLERANCE);
    }
}
