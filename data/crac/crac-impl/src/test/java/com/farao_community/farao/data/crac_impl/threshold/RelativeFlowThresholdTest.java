/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_impl.NotSynchronizedException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RelativeFlowThresholdTest {

    private double branchLimit = 20;
    private double percentageOfMax = 80;

    @Test
    public void getMinMaxValueDirect() {
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, percentageOfMax);
        assertTrue(relativeFlowThreshold.getMaxValue().isPresent());
        assertEquals(percentageOfMax, relativeFlowThreshold.getMaxValue().getAsDouble(), 0.1);
        assertFalse(relativeFlowThreshold.getMinValue().isPresent());
    }

    @Test
    public void getMinMaxValueOpposite() {
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, percentageOfMax);
        assertTrue(relativeFlowThreshold.getMinValue().isPresent());
        assertEquals(-percentageOfMax, relativeFlowThreshold.getMinValue().getAsDouble(), 0.1);
        assertFalse(relativeFlowThreshold.getMaxValue().isPresent());
    }

    @Test
    public void getMinMaxValueBoth() {
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.BOTH, percentageOfMax);
        assertTrue(relativeFlowThreshold.getMaxValue().isPresent());
        assertEquals(percentageOfMax, relativeFlowThreshold.getMaxValue().getAsDouble(), 0.1);
        assertTrue(relativeFlowThreshold.getMinValue().isPresent());
        assertEquals(-percentageOfMax, relativeFlowThreshold.getMinValue().getAsDouble(), 0.1);
    }

    @Test
    public void convertPercentToAmpere() {
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, percentageOfMax);
        relativeFlowThreshold.isSynchronized = true;
        relativeFlowThreshold.setBranchLimit(branchLimit);
        double expectedValue = branchLimit * percentageOfMax / 100;
        assertEquals(expectedValue, relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.AMPERE), 0.1);
    }

    @Test
    public void convertPercentToMegawatt() {
        RelativeFlowThreshold relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, percentageOfMax);
        try {
            relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.MEGAWATT);
            fail();
        } catch (NotSynchronizedException e) {
            relativeFlowThreshold.isSynchronized = true;
            relativeFlowThreshold.setBranchLimit(branchLimit);
            relativeFlowThreshold.voltageLevel = 400;
            double expectedValue = branchLimit * percentageOfMax / 100 * relativeFlowThreshold.voltageLevel * Math.sqrt(3) / 1000;
            assertEquals(expectedValue, relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.MEGAWATT), 0.1);
        }

    }
}
