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
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;

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

    @Test
    public void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpoint generatorSetpoint = new InjectionSetpoint(
                "id",
                "name",
                "RTE",
                Collections.emptyList(),
                new NetworkElement("FFR1AA1 _generator"),
                100
        );
        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    public void applyOnLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpoint loadSetpoint = new InjectionSetpoint(
                "id",
                "name",
                "RTE",
                Collections.emptyList(),
                new NetworkElement("FFR1AA1 _load"),
                100
        );
        loadSetpoint.apply(network);
        assertEquals(100., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
    }

    @Test
    public void applyOnDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpoint danglingLineSetpoint = new InjectionSetpoint(
                "id",
                "name",
                "RTE",
                Collections.emptyList(),
                new NetworkElement("DL1"),
                100
        );
        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    public void equals() {
        assertEquals(injectionSetpoint, injectionSetpoint);
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpoint differentInjectionSetpoint = new InjectionSetpoint("anotherId", networkElement, 12.);
        assertNotEquals(injectionSetpoint, differentInjectionSetpoint);
    }
}
