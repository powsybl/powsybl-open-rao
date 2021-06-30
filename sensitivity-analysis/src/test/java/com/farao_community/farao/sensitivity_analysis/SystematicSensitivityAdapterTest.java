/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityAdapterTest {

    @Test
    public void testWithoutAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters());

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase")), 1e-3);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2")), 1e-3);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1")), 1e-3);
        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase")), 1e-3);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2")), 1e-3);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1")), 1e-3);
        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec2basecase")), 1e-3);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec1stateCurativeContingency2")), 1e-3);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec2stateCurativeContingency1")), 1e-3);
    }

    @Test
    public void testWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        crac.newFlowCnec()
                .withId("cnec2stateOutageContingency1")
                .withNetworkElement("FFR2AA1  DDE3AA1  1")
                .withInstant(Instant.OUTAGE)
                .withContingency("Contingency FR1 FR3")
                .withOptimized(true)
                .withOperator("operator2")
                .newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-1500.)
                .withMax(1500.)
                .add()
                .newThreshold()
                .withUnit(Unit.PERCENT_IMAX)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-0.3)
                .withMax(0.3)
                .add()
                .withNominalVoltage(380.)
                .withIMax(5000.)
                .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions, new SensitivityAnalysisParameters());

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase")), 1e-3);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2")), 1e-3);
        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase")), 1e-3);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2")), 1e-3);
        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec2basecase")), 1e-3);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec1stateCurativeContingency2")), 1e-3);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1")), 1e-3);
        assertEquals(-180, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1")), 1e-3);
        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec2stateCurativeContingency1")), 1e-3);

        // for outage CNECs, do NOT take CRAs into account
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1")), 1e-3);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1")), 1e-3);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getCnec("cnec2stateOutageContingency1")), 1e-3);
    }
}
