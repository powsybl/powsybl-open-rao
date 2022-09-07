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
import com.farao_community.farao.data.crac_api.cnec.Side;
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

    public static final double DOUBLE_TOLERANCE = 1e-6;

    @Test
    public void testWithoutAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT, Side.RIGHT));
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi");

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(-25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(-205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(-5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(-5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithoutAppliedRaLeftSideOnly() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT));
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi");

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT, Side.RIGHT));
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(Instant.OUTAGE)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(Side.LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(Side.RIGHT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(Side.LEFT).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(Side.RIGHT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions, new SensitivityAnalysisParameters(), "MockSensi");

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), Side.RIGHT), DOUBLE_TOLERANCE);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-45, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-90, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-95, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-3.0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        // for outage CNECs, do NOT take CRAs into account
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), Side.LEFT), DOUBLE_TOLERANCE);
        assertEquals(-5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), Side.RIGHT), DOUBLE_TOLERANCE);
    }
}
