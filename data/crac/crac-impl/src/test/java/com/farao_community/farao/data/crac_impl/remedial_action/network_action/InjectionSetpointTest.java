/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class InjectionSetpointTest extends AbstractRemedialActionTest {

    private InjectionSetpoint injectionSetpoint;
    private double setpointValue;

    @Before
    public void setUp() throws Exception {
        String injectionSetpointId = "id";
        ArrayList<UsageRule> usageRules = createUsageRules();
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        setpointValue = 10;
        injectionSetpoint = new InjectionSetpoint(
                injectionSetpointId,
                injectionSetpointId,
                injectionSetpointId,
                usageRules,
                mockedNetworkElement,
                setpointValue
        );

    }

    @Test
    public void getSetpoint() {
        assertEquals(setpointValue, injectionSetpoint.getSetpoint(), 0);
    }

    @Test
    public void setSetpoint() {
        double newValue = 115;
        injectionSetpoint.setSetpoint(newValue);
        assertEquals(newValue, injectionSetpoint.getSetpoint(), 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void apply() {
        Network mockedNetwork = Mockito.mock(Network.class);
        injectionSetpoint.apply(mockedNetwork);
    }

    @Test
    public void equals() {
        assertEquals(injectionSetpoint, injectionSetpoint);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpoint differentInjectionSetpoint = new InjectionSetpoint("anotherId", networkElement, 12.);
        assertNotEquals(injectionSetpoint, differentInjectionSetpoint);
    }
}
