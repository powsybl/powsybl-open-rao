/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class UnitTest {

    @Test
    public void checkPhysicalParameterTestOk() {
        Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.FLOW);
        Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.ANGLE);
        Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
        Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.FLOW);
    }

    @Test
    public void checkPhysicalParameterTestNok() {

        try {
            Unit.AMPERE.checkPhysicalParameter(PhysicalParameter.ANGLE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.DEGREE.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.KILOVOLT.checkPhysicalParameter(PhysicalParameter.FLOW);
            fail();
        } catch (FaraoException e) {
            // should throw
        }

        try {
            Unit.MEGAWATT.checkPhysicalParameter(PhysicalParameter.VOLTAGE);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void testLoopflowParameters() {
        CnecLoopFlowExtension cnecLoopFlowExtension = new CnecLoopFlowExtension();
        cnecLoopFlowExtension.setLoopFlowConstraint(100);
        assertEquals(100, cnecLoopFlowExtension.getLoopFlowConstraint(), 0.1);
        assertEquals(0.0, cnecLoopFlowExtension.getInputLoopFlow(), 0.1);
    }
}
