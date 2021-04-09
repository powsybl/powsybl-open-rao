/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class NetworkActionImplTest {

    private ElementaryAction mockedElementaryAction1;
    private ElementaryAction mockedElementaryAction2;
    private UsageRule mockedUsageRule1;
    private UsageRule mockedUsageRule2;

    @Before
    public void setUp() {

        mockedUsageRule1 = Mockito.mock(UsageRule.class);
        mockedUsageRule2 = Mockito.mock(UsageRule.class);
        mockedElementaryAction1 = Mockito.mock(ElementaryAction.class);
        mockedElementaryAction2 = Mockito.mock(ElementaryAction.class);
        Mockito.when(mockedElementaryAction1.getNetworkElement()).thenReturn(new NetworkElement("ne1"));
        Mockito.when(mockedElementaryAction2.getNetworkElement()).thenReturn(new NetworkElement("ne2"));
    }

    @Test
    public void networkActionWithOneElementaryAction() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            Collections.singletonList(mockedUsageRule1),
            Collections.singleton(mockedElementaryAction1)
        );

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getUsageRules().size());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals("ne1", networkAction.getElementaryActions().iterator().next().getNetworkElement().getId());
    }

    @Test
    public void networkActionWithTwoElementaryActions() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            Arrays.asList(mockedUsageRule1, mockedUsageRule2),
            new HashSet<>(Arrays.asList(mockedElementaryAction1, mockedElementaryAction2))
        );

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(2, networkAction.getUsageRules().size());
        assertEquals(2, networkAction.getElementaryActions().size());
    }
}
