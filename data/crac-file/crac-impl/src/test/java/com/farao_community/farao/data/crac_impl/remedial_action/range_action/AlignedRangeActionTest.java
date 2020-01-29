/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class AlignedRangeActionTest {

    private AlignedRangeAction alignedRangeAction;
    private Set<NetworkElement> mockedNetworkElements;
    private List<Range> mockedRanges;
    private Network mockedNetwork;

    @Before
    public void setUp() {
        String complexRangeActionId = "cra_id";

        String complexRangeActionName = "cra_name";

        String complexRangeActionOperator = "cra_operator";

        UsageRule freeToUse = Mockito.mock(FreeToUse.class);
        List<UsageRule> usageRuleList = new ArrayList<>(Collections.singletonList(freeToUse));

        mockedNetwork = Mockito.mock(Network.class);
        Range range = Mockito.mock(Range.class);
        Mockito.when(range.getMax()).thenReturn(AlignedRangeAction.TEMP_MAX_VALUE);
        Mockito.when(range.getMin()).thenReturn(AlignedRangeAction.TEMP_MIN_VALUE);

        mockedRanges = new ArrayList<>(Collections.singletonList(range));

        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        mockedNetworkElements = new HashSet<>();
        mockedNetworkElements.add(mockedNetworkElement);

        alignedRangeAction = new AlignedRangeAction(
                complexRangeActionId,
                complexRangeActionName,
                complexRangeActionOperator,
                usageRuleList,
                mockedRanges,
                mockedNetworkElements
        );

    }

    @Test
    public void getRanges() {
        assertEquals(mockedRanges, alignedRangeAction.getRanges());
    }

    @Test
    public void getNetworkElements() {
        assertEquals(mockedNetworkElements, alignedRangeAction.getNetworkElements());
    }

    @Test
    public void getMinValue() {
        assertEquals(AlignedRangeAction.TEMP_MIN_VALUE, alignedRangeAction.getMinValue(mockedNetwork), 0.1);
    }

    @Test
    public void getMaxValue() {
        assertEquals(AlignedRangeAction.TEMP_MAX_VALUE, alignedRangeAction.getMaxValue(mockedNetwork), 0.1);
    }

    @Test
    public void apply() {
        try {
            alignedRangeAction.apply(mockedNetwork, 5);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void addRange() {
        Range anotherRange = Mockito.mock(Range.class);
        alignedRangeAction.addRange(anotherRange);
        assertEquals(2, alignedRangeAction.getRanges().size());
    }

    @Test
    public void addNetworkElement() {
        NetworkElement anotherMockedNetworkElement = Mockito.mock(NetworkElement.class);
        mockedNetworkElements.add(anotherMockedNetworkElement);
        assertEquals(2, alignedRangeAction.getNetworkElements().size());
    }
}
