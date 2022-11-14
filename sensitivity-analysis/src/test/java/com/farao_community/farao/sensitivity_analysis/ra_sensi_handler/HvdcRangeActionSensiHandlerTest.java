/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionSensiHandlerTest {

    @Test
    public void checkConsistencyOKTest() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    public void getSensitivityOnFlowTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA11 FFR3AA11 1", flowCnec, Side.LEFT)).thenReturn(-12.56);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA11 FFR3AA11 1", flowCnec, Side.RIGHT)).thenReturn(-10.56);

        assertEquals(-12.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.LEFT, sensiResult), 1e-3);
        assertEquals(-10.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.RIGHT, sensiResult), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotAHvdc() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac");

        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE1AA11 BBE2AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        sensiHandler.checkConsistency(network); // should throw
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotANetworkElement() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac");

        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("unknownNetworkElement")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        sensiHandler.checkConsistency(network); // should throw
    }
}
