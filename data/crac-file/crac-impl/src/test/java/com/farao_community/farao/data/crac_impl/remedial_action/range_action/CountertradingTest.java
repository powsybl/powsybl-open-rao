/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.UsageRule;
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
public class CountertradingTest extends AbstractRemedialActionTest {

    private Countertrading countertrading;
    private Network mockedNetwork;

    @Before
    public void setUp() throws Exception {
        String id = "countertrading_id";
        UsageRule mockedUsageRule = Mockito.mock(UsageRule.class);
        ArrayList<UsageRule> usageRules = new ArrayList<>();
        usageRules.add(mockedUsageRule);
        countertrading = new Countertrading(id, id, id, usageRules);
        mockedNetwork = Mockito.mock(Network.class);
    }

    @Test
    public void getMinAndMaxValue() {
        assertEquals(Countertrading.TEMP_VALUE, countertrading.getMinValue(mockedNetwork), 0);
        assertEquals(Countertrading.TEMP_VALUE, countertrading.getMaxValue(mockedNetwork), 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void apply() {
        double anySetpoint = 123.4;
        countertrading.apply(mockedNetwork, anySetpoint);
    }

    @Test
    public void getNetworkElements() {
        assertEquals(0, countertrading.getNetworkElements().size());
    }
}
