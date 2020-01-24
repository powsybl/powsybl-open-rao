/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class HvdcRangeTest extends AbstractNetworkElementRangeActionTest {

    private HvdcRange hvdcRange;

    @Before
    public void setUp() throws Exception {
        String hvdcRangeId = "id";
        ArrayList<UsageRule> usageRules = createUsageRules();
        ArrayList<AbstractRange> ranges = createRanges();
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        hvdcRange = new HvdcRange(
                hvdcRangeId,
                hvdcRangeId,
                hvdcRangeId,
                usageRules,
                ranges,
                mockedNetworkElement
        );
    }

    @Test
    public void getMinAndMaxValueWithSingleRange() {
        Network mockedNetwork = Mockito.mock(Network.class);
        AbstractRange anyRange = Mockito.mock(AbstractRange.class);
        assertEquals(HvdcRange.tempValue, hvdcRange.getMaxValueWithRange(mockedNetwork, anyRange), 0);
        assertEquals(HvdcRange.tempValue, hvdcRange.getMinValueWithRange(mockedNetwork, anyRange), 0);
    }

    /*@Test
    public void getMinAndMaxValueWithMultipleRanges() {
        AbsoluteFixedRange range1 = Mockito.mock(AbsoluteFixedRange.class);
        RelativeFixedRange range2 = Mockito.mock(RelativeFixedRange.class);
        hvdcRange.addRange(range1);
        hvdcRange.addRange(range2);

        Network mockedNetwork = Mockito.mock(Network.class);

        AbstractRange initialRange = hvdcRange.ranges.get(0);
        double expectedMinRange0 = -100;
        Mockito.when(hvdcRange.getMinValueWithRange(mockedNetwork, initialRange)).thenReturn(expectedMinRange0);

        double expectedMinRange1 = -5;
        Mockito.when(hvdcRange.getMinValueWithRange(mockedNetwork, range1)).thenReturn(expectedMinRange1);

        double expectedMinRange2 = -10;
        Mockito.when(hvdcRange.getMinValueWithRange(mockedNetwork, range2)).thenReturn(expectedMinRange2);

        assertEquals(Math.max(expectedMinRange1, expectedMinRange2), hvdcRange.getMaxValue(mockedNetwork), 0);
    }*/

    @Test(expected = UnsupportedOperationException.class)
    public void apply() {
        Network mockedNetwork = Mockito.mock(Network.class);
        double anySetpoint = 123.4;
        hvdcRange.apply(mockedNetwork, anySetpoint);
    }

}
