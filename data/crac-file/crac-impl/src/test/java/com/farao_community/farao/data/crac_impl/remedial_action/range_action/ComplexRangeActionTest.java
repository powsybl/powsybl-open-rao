/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.ApplicableRangeAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.AbsoluteFixedRange;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.farao_community.farao.data.crac_impl.range_domain.RelativeFixedRange;
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
public class ComplexRangeActionTest {

    private ComplexRangeAction complexRangeAction;
    private ApplicableRangeAction mockedApplicableRangeAction;
    private ApplicableRangeAction anotherMockedApplicableRangeAction;
    private Set<NetworkElement> mockedNetworkElements;
    private List<AbstractRange> mockedRanges;
    private Network mockedNetwork;

    @Before
    public void setUp() {
        String complexRangeActionId = "cra_id";

        String complexRangeActionName = "cra_name";

        String complexRangeActionOperator = "cra_operator";

        UsageRule freeToUse = Mockito.mock(FreeToUse.class);
        List<UsageRule> usageRuleList = new ArrayList<>(Collections.singletonList(freeToUse));

        mockedNetwork = Mockito.mock(Network.class);
        AbsoluteFixedRange range = Mockito.mock(AbsoluteFixedRange.class);
        Mockito.when(range.getMaxValue(mockedNetwork)).thenReturn(ComplexRangeAction.TEMP_MAX_VALUE);
        Mockito.when(range.getMinValue(mockedNetwork)).thenReturn(ComplexRangeAction.TEMP_MIN_VALUE);
        mockedRanges = new ArrayList<>(Collections.singletonList(range));

        mockedApplicableRangeAction = Mockito.mock(PstRange.class);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        mockedNetworkElements = new HashSet<>(Collections.singleton(networkElement));
        Mockito.when(mockedApplicableRangeAction.getNetworkElements()).thenReturn(mockedNetworkElements);
        Set<ApplicableRangeAction> applicableRangeActions = Collections.singleton(mockedApplicableRangeAction);

        complexRangeAction = new ComplexRangeAction(
                complexRangeActionId,
                complexRangeActionName,
                complexRangeActionOperator,
                usageRuleList,
                mockedRanges,
                applicableRangeActions
        );

    }

    @Test
    public void getRanges() {
        assertEquals(mockedRanges, complexRangeAction.getRanges());
    }

    @Test
    public void getNetworkElements() {
        assertEquals(mockedNetworkElements, complexRangeAction.getNetworkElements());
    }

    @Test
    public void getMinValue() {
        assertEquals(ComplexRangeAction.TEMP_MIN_VALUE, complexRangeAction.getMinValue(mockedNetwork), 0.1);
    }

    @Test
    public void getMaxValue() {
        assertEquals(ComplexRangeAction.TEMP_MAX_VALUE, complexRangeAction.getMaxValue(mockedNetwork), 0.1);
    }

    @Test
    public void apply() {
        try {
            complexRangeAction.apply(mockedNetwork, 5);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void addRange() {
        RelativeFixedRange anotherRange = Mockito.mock(RelativeFixedRange.class);
        complexRangeAction.addRange(anotherRange);
        assertEquals(2, complexRangeAction.getRanges().size());
    }

    @Test
    public void addApplicableRangeAction() {
        anotherMockedApplicableRangeAction = Mockito.mock(HvdcRange.class);
        complexRangeAction.addApplicableRangeAction(anotherMockedApplicableRangeAction);
        assertEquals(2, complexRangeAction.getApplicableRangeActions().size());
    }

    @Test
    public void getApplicableRangeActions() {
        complexRangeAction.addApplicableRangeAction(anotherMockedApplicableRangeAction);
        Set<ApplicableRangeAction> expectedSet = new HashSet<>(Collections.singletonList(mockedApplicableRangeAction));
        expectedSet.add(mockedApplicableRangeAction);
        expectedSet.add(anotherMockedApplicableRangeAction);
        assertEquals(expectedSet, complexRangeAction.getApplicableRangeActions());
    }
}
