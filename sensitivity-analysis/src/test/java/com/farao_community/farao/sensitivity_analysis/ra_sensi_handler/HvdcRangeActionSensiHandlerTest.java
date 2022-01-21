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
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.HvdcSetpointIncrease;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class HvdcRangeActionSensiHandlerTest {

    @Test
    public void checkConsistencyOKTest() {
        Network network = Importers.loadNetwork("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
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
    public void rangeActionToSensitivityVariableTest() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        List<SensitivityVariable> sensiVariables = sensiHandler.rangeActionToSensitivityVariable();
        assertEquals(1, sensiVariables.size());
        assertTrue(sensiVariables.get(0) instanceof HvdcSetpointIncrease);
        assertEquals("BBE2AA11 FFR3AA11 1", sensiVariables.get(0).getId());
        assertEquals("BBE2AA11 FFR3AA11 1", ((HvdcSetpointIncrease) sensiVariables.get(0)).getHvdcId());
    }

    @Test
    public void getSensitivityOnFlowTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec anyFLowCnec = crac.getFlowCnec("cnec1basecase");
        HvdcRangeAction hvdcRangeAction = crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA11 FFR3AA11 1", anyFLowCnec)).thenReturn(-12.56);

        assertEquals(-12.56, sensiHandler.getSensitivityOnFlow(anyFLowCnec, sensiResult), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotAHvdc() {
        Network network = Importers.loadNetwork("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
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
        Network network = Importers.loadNetwork("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
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
