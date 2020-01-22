/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.*;
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
public class ComplexNetworkActionTest {

    private ComplexNetworkAction complexNetworkAction;
    private NetworkAction mockedNetworkAction;
    private Set<NetworkElement> mockedNetworkElements;

    @Before
    public void setUp() {
        String complexNetworkActionId = "cna_id";

        String complexNetworkActionName = "cna_name";

        String complexNetworkActionOperator = "cna_operator";

        UsageRule freeToUse = Mockito.mock(FreeToUse.class);
        List<UsageRule> usageRuleList = new ArrayList<>(Collections.singletonList(freeToUse));

        mockedNetworkAction = Mockito.mock(PstSetpoint.class);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        mockedNetworkElements = new HashSet<>(Collections.singleton(networkElement));
        Mockito.when(mockedNetworkAction.getNetworkElements()).thenReturn(mockedNetworkElements);
        Set<NetworkAction> applicableNetworkActions = Collections.singleton(mockedNetworkAction);

        complexNetworkAction = new ComplexNetworkAction(
                complexNetworkActionId,
                complexNetworkActionName,
                complexNetworkActionOperator,
                usageRuleList,
                applicableNetworkActions
                );
    }

    @Test
    public void getApplicableNetworkActions() {

        Set<NetworkAction> applicableNetworkActions = Collections.singleton(mockedNetworkAction);
        assertEquals(
                applicableNetworkActions,
                complexNetworkAction.getNetworkActions()
        );
    }

    @Test
    public void apply() {
        Network mockedNetwork = Mockito.mock(Network.class);
        try {
            complexNetworkAction.apply(mockedNetwork);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void getNetworkElements() {
        assertEquals(mockedNetworkElements, complexNetworkAction.getNetworkElements());
    }

    @Test
    public void addNetworkAction() {
        NetworkAction anotherNetworkAction = Mockito.mock(NetworkAction.class);
        complexNetworkAction.addApplicableNetworkAction(anotherNetworkAction);
        Set<NetworkAction> expectedNetworkActions = new HashSet<>();
        expectedNetworkActions.add(mockedNetworkAction);
        expectedNetworkActions.add(anotherNetworkAction);
        assertEquals(expectedNetworkActions, complexNetworkAction.getNetworkActions());

    }
}
