/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class HvdcSetpointTest extends AbstractRemedialActionTest {

    private HvdcSetpoint hvdcSetpoint;
    private double hvdcSetpointValue;

    @Before
    public void setUp() {
        String hvdcSetpointId = "id";
        ArrayList<UsageRule> usageRules = createUsageRules();
        NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        hvdcSetpoint = new HvdcSetpoint(hvdcSetpointId, networkElement, hvdcSetpointValue);

        hvdcSetpointValue = 22;
        hvdcSetpoint = new HvdcSetpoint(
                hvdcSetpointId,
                hvdcSetpointId,
                hvdcSetpointId,
                usageRules,
                networkElement,
                hvdcSetpointValue
        );
    }

    @Test
    public void getSetpoint() {
        assertEquals(hvdcSetpointValue, hvdcSetpoint.getSetpoint(), 0);
    }

    @Test
    public void setSetpoint() {
        double hvdcSetpointNewValue = 12.4;
        hvdcSetpoint.setSetpoint(hvdcSetpointNewValue);
        assertEquals(hvdcSetpointNewValue, hvdcSetpoint.getSetpoint(), 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void apply() {
        Network mockedNetwork = Mockito.mock(Network.class);
        hvdcSetpoint.apply(mockedNetwork);
    }
}
