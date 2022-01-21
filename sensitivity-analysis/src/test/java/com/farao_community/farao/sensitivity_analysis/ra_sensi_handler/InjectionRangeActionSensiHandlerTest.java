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
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionRangeActionSensiHandlerTest {

    @Test
    public void checkConsistencyOKTest() {
        Network network = Importers.loadNetwork("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    public void rangeActionToSensitivityVariableSimpleTest() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE2AA12_generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        List<SensitivityVariable> sensiVariables = sensiHandler.rangeActionToSensitivityVariable();
        assertEquals(1, sensiVariables.size());

        // positive injections
        assertTrue(sensiVariables.get(0) instanceof LinearGlsk);
        assertEquals("injectionRangeId-positiveInjections", sensiVariables.get(0).getId());
        assertEquals(1, ((LinearGlsk) sensiVariables.get(0)).getGLSKs().size());
        assertEquals(1, ((LinearGlsk) sensiVariables.get(0)).getGLSKs().get("BBE2AA12_generator"), 1e-3);
    }

    @Test
    public void rangeActionToSensitivityVariableComplexTest() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        List<SensitivityVariable> sensiVariables = sensiHandler.rangeActionToSensitivityVariable();
        assertEquals(2, sensiVariables.size());

        // positive injections
        assertTrue(sensiVariables.get(0) instanceof LinearGlsk);
        assertEquals("injectionRangeId-positiveInjections", sensiVariables.get(0).getId());
        assertEquals(2, ((LinearGlsk) sensiVariables.get(0)).getGLSKs().size());
        assertEquals(0.5, ((LinearGlsk) sensiVariables.get(0)).getGLSKs().get("BBE2AA12_generator"), 1e-3);
        assertEquals(0.5, ((LinearGlsk) sensiVariables.get(0)).getGLSKs().get("BBE2AA12_load"), 1e-3);

        // negative injections
        assertTrue(sensiVariables.get(1) instanceof LinearGlsk);
        assertEquals("injectionRangeId-negativeInjections", sensiVariables.get(1).getId());
        assertEquals(2, ((LinearGlsk) sensiVariables.get(1)).getGLSKs().size());
        assertEquals(0.4, ((LinearGlsk) sensiVariables.get(1)).getGLSKs().get("FFR3AA12_generator"), 1e-3);
        assertEquals(0.6, ((LinearGlsk) sensiVariables.get(1)).getGLSKs().get("FFR3AA12_load"), 1e-3);
    }

    @Test
    public void getSensitivityOnFlowSimpleTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec anyFLowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE2AA12_generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", anyFLowCnec)).thenReturn(-1.56);

        assertEquals(-1.56, sensiHandler.getSensitivityOnFlow(anyFLowCnec, sensiResult), 1e-3);
    }

    @Test
    public void getSensitivityOnFlowComplexTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec anyFLowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", anyFLowCnec)).thenReturn(4.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-negativeInjections", anyFLowCnec)).thenReturn(7.);

        assertEquals(4 * 0.8 - 7 * 0.5, sensiHandler.getSensitivityOnFlow(anyFLowCnec, sensiResult), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotAnInjection() {
        Network network = Importers.loadNetwork("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE1AA11 BBE2AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        sensiHandler.checkConsistency(network); // should throw
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotANetworkElement() {
        Network network = Importers.loadNetwork("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "unknown")
                .newRange().withMin(-1000).withMax(1000).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        sensiHandler.checkConsistency(network); // should throw
    }
}
