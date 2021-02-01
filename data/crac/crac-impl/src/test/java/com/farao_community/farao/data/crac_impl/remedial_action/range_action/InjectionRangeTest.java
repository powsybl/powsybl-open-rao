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
import com.farao_community.farao.data.crac_api.Range;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class InjectionRangeTest extends AbstractRangeActionTest {

    private InjectionRange injectionRange;

    @Before
    public void setUp() throws Exception {
        String injectionRangeId = "id";
        ArrayList<UsageRule> usageRules = createUsageRules();
        List<Range> ranges = createRanges();
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        injectionRange = new InjectionRange(
                injectionRangeId,
                injectionRangeId,
                injectionRangeId,
                usageRules,
                ranges,
                mockedNetworkElement
        );
    }

    @Test
    public void getMinAndMaxValueWithRange() {
        Network mockedNetwork = Mockito.mock(Network.class);
        Range anyRange = Mockito.mock(Range.class);
        assertEquals(InjectionRange.injectionRangeTempValue, injectionRange.getMaxValueWithRange(mockedNetwork, anyRange), 0);
        assertEquals(InjectionRange.injectionRangeTempValue, injectionRange.getMinValueWithRange(mockedNetwork, anyRange), 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void apply() {
        Network mockedNetwork = Mockito.mock(Network.class);
        double anySetpointValue = 1231;
        injectionRange.apply(mockedNetwork, anySetpointValue);
    }
}
