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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RelativeFlowThresholdTest {

    private RelativeFlowThreshold relativeFlowThreshold;
    private double branchLimit;
    private double percentageOfMax;

    @Before
    public void setUp() {
        branchLimit = 20;
        percentageOfMax = 80;
        relativeFlowThreshold = new RelativeFlowThreshold(Side.LEFT, Direction.DIRECT, percentageOfMax);
    }

    @Test
    public void convertPercentToAmpere() {
        relativeFlowThreshold.isSynchronized = true;
        relativeFlowThreshold.setBranchLimit(branchLimit);
        double expectedValue = branchLimit * percentageOfMax / 100;
        assertEquals(expectedValue, relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.AMPERE), 0.1);
    }

    @Test
    public void convertPercentToMegawatt() {
        try {
            relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.MEGAWATT);
            fail();
        } catch (NotSynchronizedException e) {
            relativeFlowThreshold.isSynchronized = true;
            relativeFlowThreshold.setBranchLimit(branchLimit);
            relativeFlowThreshold.voltageLevel = 400;
            double expectedValue = branchLimit * percentageOfMax / 100 * relativeFlowThreshold.voltageLevel * Math.sqrt(3) / 1000;
            assertEquals(expectedValue, relativeFlowThreshold.convert(100, Unit.PERCENT_IMAX, Unit.MEGAWATT), 0.1);
            // do nothing
        }

    }
}
