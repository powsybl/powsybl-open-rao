/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.data.cracapi.cnec.Side.RIGHT;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SystematicSensitivityAdapterTest {

    public static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testWithoutAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT, RIGHT));
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi", outageInstant);

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT, null), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT, null), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithoutAppliedRaLeftSideOnly() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT));
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, new SensitivityAnalysisParameters(), "MockSensi", outageInstant);

        // "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getFlowCnec("cnec1stateCurativeContingency2").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(0, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT, null), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT, null), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
    }

    @Test
    void testWithAppliedRa() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(LEFT, RIGHT));
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newFlowCnec()
            .withId("cnec2stateOutageContingency1")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(LEFT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(RIGHT).withMin(-1500.).withMax(1500.).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(LEFT).withMin(-0.3).withMax(0.3).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(RIGHT).withMin(-0.3).withMax(0.3).add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getPstRangeAction("pst"), -3.1);

        SystematicSensitivityResult result = SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions, new SensitivityAnalysisParameters(), "MockSensi", crac.getOutageInstant());

        // after initial state or contingency without CRA, "standard results" of the MockSensiProvider are expected
        assertEquals(10, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-15, result.getReferenceFlow(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getInstant(OUTAGE_INSTANT_ID)), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getInstant(OUTAGE_INSTANT_ID)), DOUBLE_TOLERANCE);

        assertEquals(25, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), LEFT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(-30, result.getReferenceIntensity(crac.getFlowCnec("cnec2basecase"), RIGHT, crac.getFlowCnec("cnec2basecase").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getInstant(OUTAGE_INSTANT_ID)), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getInstant(OUTAGE_INSTANT_ID)), DOUBLE_TOLERANCE);

        assertEquals(0.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), LEFT, null), DOUBLE_TOLERANCE);
        assertEquals(-0.55, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2basecase"), RIGHT, null), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), LEFT, crac.getInstant("outage")), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec1stateCurativeContingency2"), RIGHT, crac.getInstant("outage")), DOUBLE_TOLERANCE);

        // after contingency with CRA, "alternative" results of the MockSensiProvider are expected
        assertEquals(-40, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(45, result.getReferenceFlow(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-90, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(95, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getFlowCnec("cnec2stateCurativeContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-2.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(3.0, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateCurativeContingency1"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);

        // for outage CNECs, do NOT take CRAs into account
        assertEquals(-20, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT, crac.getFlowCnec("cnec2stateOutageContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(25, result.getReferenceFlow(crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT, crac.getFlowCnec("cnec2stateOutageContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-200, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT, crac.getFlowCnec("cnec2stateOutageContingency1").getState().getInstant()), DOUBLE_TOLERANCE);
        assertEquals(205, result.getReferenceIntensity(crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT, crac.getFlowCnec("cnec2stateOutageContingency1").getState().getInstant()), DOUBLE_TOLERANCE);

        assertEquals(-5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), LEFT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
        assertEquals(5.5, result.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getFlowCnec("cnec2stateOutageContingency1"), RIGHT, crac.getInstant("curative")), DOUBLE_TOLERANCE);
    }
}
