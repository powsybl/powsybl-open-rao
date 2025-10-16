/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class LoopFlowThresholdAdderImplTest {

    private FlowCnec flowCnec;

    @BeforeEach
    public void setUp() {

        Crac crac = CracFactory.findDefault().create("cracId", "cracName")
            .newInstant("preventive", InstantKind.PREVENTIVE);
        flowCnec = crac.newFlowCnec()
            .withId("flowCnecId")
            .withName("flowCnecName")
            .withNetworkElement("networkElementId")
            .withInstant("preventive")
            .withOperator("operator")
            .withOptimized(true)
            .newThreshold()
                .withSide(TwoSides.ONE)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .withMin(-1000.0)
                .add()
            .add();
    }

    @Test
    void addLoopFlowThreshold() {

        flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(100.0)
            .add();

        LoopFlowThreshold loopFlowThreshold = flowCnec.getExtension(LoopFlowThreshold.class);
        assertNotNull(loopFlowThreshold);
        assertEquals(Unit.MEGAWATT, loopFlowThreshold.getUnit());
        assertEquals(100.0, loopFlowThreshold.getValue(), 1e-3);
    }

    @Test
    void addLoopFlowThresholdNoValue() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT);
        OpenRaoException exception = assertThrows(OpenRaoException.class, loopFlowThresholdAdder::add);
        assertEquals("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdNoUnit() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withValue(100.0);
        OpenRaoException exception = assertThrows(OpenRaoException.class, loopFlowThresholdAdder::add);
        assertEquals("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdNegativeThreshold() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.MEGAWATT)
            .withValue(-100.0);
        OpenRaoException exception = assertThrows(OpenRaoException.class, loopFlowThresholdAdder::add);
        assertEquals("LoopFlowThresholds must have a positive threshold.", exception.getMessage());
    }

    @Test
    void addLoopFlowThresholdPercentGreaterThanOne() {
        LoopFlowThresholdAdder loopFlowThresholdAdder = flowCnec.newExtension(LoopFlowThresholdAdder.class)
            .withUnit(Unit.PERCENT_IMAX)
            .withValue(25);
        OpenRaoException exception = assertThrows(OpenRaoException.class, loopFlowThresholdAdder::add);
        assertEquals("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.", exception.getMessage());
    }
}
