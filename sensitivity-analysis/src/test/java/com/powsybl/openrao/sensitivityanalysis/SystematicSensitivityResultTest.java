/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.*;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Network network;
    private FlowCnec nStateCnec;
    private FlowCnec contingencyCnec;
    private RangeAction<?> rangeAction;
    private HvdcRangeAction hvdcRangeAction;
    private SensitivityVariableSet linearGlsk;
    private Crac crac;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;
    private int outageInstantOrder;

    public void setUpWith12Nodes() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange(Set.of(TwoSides.ONE, TwoSides.TWO));
        outageInstantOrder = crac.getInstant(CURATIVE_INSTANT_ID).getOrder();

        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
            .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));

        // Ra Provider
        rangeActionSensitivityProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Ptdf Provider
        ptdfSensitivityProvider = new PtdfSensitivityProvider(glskProvider, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        nStateCnec = crac.getFlowCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        contingencyCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        linearGlsk = glskProvider.getData("10YFR-RTE------C");
    }

    @Test
    void testPostTreatIntensities() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder);

        // Before postTreating intensities
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);

        // After postTreating intensities
        result.postTreatIntensities();
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPstResultManipulation() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-30, result.getReferenceIntensity(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-0.55, result.getSensitivityOnFlow(rangeAction, nStateCnec, TwoSides.TWO), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(5.5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPtdfResultManipulation() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            ptdfSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, TwoSides.TWO), EPSILON);
        assertEquals(-0.19, result.getSensitivityOnFlow(linearGlsk, nStateCnec, TwoSides.TWO), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, TwoSides.ONE), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-6.5, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testFailureSensiResult() {
        setUpWith12Nodes();
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // Then
        assertFalse(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    private void setUpForHvdc() {
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        crac.newContingency()
            .withId("co")
            .withContingencyElement("NNL2AA11 BBE3AA11 1", ContingencyElementType.LINE)
            .add();
        nStateCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .add();
        contingencyCnec = crac.newFlowCnec()
            .withId("cnec-cur")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withContingency("co")
            .withInstant(OUTAGE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.TWO).add()
            .add();
        hvdcRangeAction = crac.newHvdcRangeAction()
            .withId("hvdc-ra")
            .withNetworkElement("BBE2AA11 FFR3AA11 1")
            .newRange().withMin(-1000.).withMax(1000.).add()
            .add();

        network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        rangeActionSensitivityProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));
    }

    @Test
    void testPostTreatHvdcNoEffect() {
        setUpForHvdc();
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElementId(), hvdcRangeAction);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult()
            .completeData(sensitivityAnalysisResult, outageInstantOrder)
            .postTreatIntensities()
            .postTreatHvdcs(network, hvdcs);

        assertEquals(30., result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, TwoSides.ONE), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPostTreatHvdcInvert() {
        setUpForHvdc();
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElementId(), hvdcRangeAction);
        network.getHvdcLine("BBE2AA11 FFR3AA11 1").setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult()
            .completeData(sensitivityAnalysisResult, outageInstantOrder)
            .postTreatIntensities()
            .postTreatHvdcs(network, hvdcs);

        assertEquals(30., result.getReferenceFlow(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, TwoSides.ONE), EPSILON);
        assertEquals(-0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, TwoSides.ONE), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, TwoSides.TWO), EPSILON);
        assertEquals(7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, TwoSides.TWO), EPSILON);
    }

    @Test
    void testPartialContingencyFailures() {
        // Test case where N succeeds and all N-1 fail
        setUpWith12Nodes();

        // The contingency points to an element that does not exist in the network => n-1 status should be set to failure
        Contingency contingency = new Contingency("wrong_contingency", new BranchContingency("fake_branch"));
        State contingencyState = Mockito.mock(State.class);
        Mockito.when(contingencyState.getContingency()).thenReturn(Optional.of(contingency));
        Mockito.when(contingencyState.getInstant()).thenReturn(crac.getOutageInstant());

        SensitivityFactor sensitivityFactor1 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(contingency.getId(), ContingencyContextType.SPECIFIC)
        );
        SensitivityFactor sensitivityFactor2 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(null, ContingencyContextType.NONE)
        );

        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            List.of(sensitivityFactor1, sensitivityFactor2),
            List.of(contingency),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder).postTreatIntensities();

        // N is in SUCCESS, N-1 in failure
        assertTrue(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(contingencyState));
    }

    //This test simulates what happens after a second preventive, where some curative results are stored in outage results.
    //We do this to avoid recomputing multiple times the same contingency while no remedial action was applied in later instants.
    @Test
    void testCurativeResultAtOutageInstant() {
        setUpWith12Nodes();

        FlowCnecAdder cnecAdder = crac.newFlowCnec()
            .withId("cnec2stateOutageContingency2")
            .withNetworkElement("FFR2AA1  DDE3AA1  1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator2")
            .withReliabilityMargin(95.)
            .withNominalVoltage(380.)
            .withIMax(5000.);
        Set.of(TwoSides.ONE, TwoSides.TWO).forEach(side ->
            cnecAdder.newThreshold()
                .withUnit(Unit.MEGAWATT)
                .withSide(TwoSides.ONE)
                .withMin(-1800.)
                .withMax(1800.)
                .add());
        cnecAdder.add();

        outageInstantOrder = crac.getInstant(OUTAGE_INSTANT_ID).getOrder();
        int curativeInstantOrder = crac.getInstant(CURATIVE_INSTANT_ID).getOrder();

        //run sensi on outage instant for outageCnec
        FlowCnec outageCnec = crac.getFlowCnec("cnec2stateOutageContingency2");
        Contingency outageContingency = outageCnec.getState().getContingency().orElseThrow();
        SensitivityFactor outageSensitivityFactor = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            outageCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(outageContingency.getId(), ContingencyContextType.SPECIFIC)
        );
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            List.of(outageSensitivityFactor),
            List.of(outageContingency),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder);

        //run sensi on curative instant for curativeCnec
        FlowCnec curativeCnec = crac.getFlowCnec("cnec1stateCurativeContingency1");
        Contingency curativeContingency = curativeCnec.getState().getContingency().orElseThrow();
        SensitivityFactor curativeSensitivityFactor = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            curativeCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(curativeContingency.getId(), ContingencyContextType.SPECIFIC)
        );
        sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            List.of(curativeSensitivityFactor),
            List.of(curativeContingency),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        result.completeData(sensitivityAnalysisResult, curativeInstantOrder);

        //correct flows are available for both factors for all instants
        //0 represents a missing value (this is due to the way load flow engines represent an open line, we need 0 as a default value)
        assertEquals(-20, result.getReferenceFlow(outageCnec, TwoSides.ONE));
        assertEquals(-20, result.getReferenceFlow(outageCnec, TwoSides.ONE, crac.getInstant(OUTAGE_INSTANT_ID)));
        assertEquals(-20, result.getReferenceFlow(outageCnec, TwoSides.ONE, crac.getInstant(AUTO_INSTANT_ID)));
        assertEquals(-20, result.getReferenceFlow(outageCnec, TwoSides.ONE, crac.getInstant(CURATIVE_INSTANT_ID)));

        assertEquals(-20, result.getReferenceFlow(curativeCnec, TwoSides.ONE));
        assertEquals(0, result.getReferenceFlow(curativeCnec, TwoSides.ONE, crac.getInstant(OUTAGE_INSTANT_ID)));
        assertEquals(0, result.getReferenceFlow(curativeCnec, TwoSides.ONE, crac.getInstant(AUTO_INSTANT_ID)));
        assertEquals(-20, result.getReferenceFlow(curativeCnec, TwoSides.ONE, crac.getInstant(CURATIVE_INSTANT_ID)));

    }

    @Test
    void testCompleteDataWithFailingPerimeterIn2P() {

        setUpWith12Nodes();

        // A first call to complete data with a sensitivity analysis N and N-1 succeed.
        // Simulate the first call to completeData of the sensi computation pre 2P.
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());

        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, outageInstantOrder);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());

        // Simulate a second call to completeData during sensitivity computation pre 2P on a state with RAs that failed (a N-1 state fails).
        Contingency contingency = new Contingency("contingency", new BranchContingency("branch"));

        SensitivityFactor sensitivityFactor1 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(contingency.getId(), ContingencyContextType.SPECIFIC)
        );

        sensitivityAnalysisResult = new SensitivityAnalysisResult(
            List.of(sensitivityFactor1),
            List.of(new SensitivityStateStatus(SensitivityState.postContingency(contingency.getId()),
                List.of(new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.FAILED, "")))),
            List.of(contingency.getId()),
            List.of(),
            List.of(),
            true
        );

        result.completeData(sensitivityAnalysisResult, outageInstantOrder);

        //  success in n and a n-1 fails -> PARTIAL_FAILURE
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
    }

    @Test
    void testFailingCurativePerimeter() {
        setUpWith12Nodes();
        // Simulate a second call to completeData during sensitivity computation pre 2P on a state with RAs that failed (a N-1 state fails).
        Contingency contingency = new Contingency("contingency", new BranchContingency("branch"));

        SensitivityFactor sensitivityFactor1 = new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1,
            "BBE2AA1  FFR3AA1  1",
            SensitivityVariableType.TRANSFORMER_PHASE,
            "BBE2AA1  BBE3AA1  1",
            false,
            new ContingencyContext(contingency.getId(), ContingencyContextType.SPECIFIC)
        );

        SensitivityAnalysisResult sensitivityAnalysisResult = new SensitivityAnalysisResult(
            List.of(sensitivityFactor1),
            List.of(new SensitivityStateStatus(SensitivityState.postContingency(contingency.getId()),
                List.of(new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.FAILED, "")))),
            List.of(contingency.getId()),
            List.of(),
            List.of(),
            true
        );

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        result.completeData(sensitivityAnalysisResult, outageInstantOrder);

        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    @Test
    void testUnrelatedBaseComponentFailure() {
        // A failure in any base-case component invalidates all base-case values, even if another component converged.
        setUpWith12Nodes();
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, nStateCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.none());
        // Component numbers are simulated; no separate network component is constructed for this test.
        SensitivityStateStatus.ComponentStatus converged = new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, "");
        SensitivityStateStatus.ComponentStatus unrelatedFailure = new SensitivityStateStatus.ComponentStatus(1, 1, LoadFlowResult.ComponentResult.Status.FAILED, "");
        SensitivityAnalysisResult input = new SensitivityAnalysisResult(List.of(factor),
            List.of(new SensitivityStateStatus(SensitivityState.PRE_CONTINGENCY, List.of(converged, unrelatedFailure))),
            List.of(), List.of(), List.of(new SensitivityValue(0, -1, -1, 1, 2)), true);

        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, outageInstantOrder);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(crac.getPreventiveState()));
        assertTrue(Double.isNaN(result.getReferenceFlow(nStateCnec, TwoSides.ONE)));
        assertTrue(Double.isNaN(result.getSensitivityOnFlow("pst", nStateCnec, TwoSides.ONE)));
    }

    @Test
    void testComponentStatusAggregation() {
        // Post-contingency component statuses determine whether the state succeeds or its values become NaN.
        setUpWith12Nodes();
        String contingencyId = contingencyCnec.getState().getContingency().orElseThrow().getId();
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, nStateCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.all());
        // Factor index 0 refers to the only factor; -1 means no contingency or operator strategy.
        // Contingency index 0 refers to the first contingency; the last two arguments are sensitivity and reference flow.
        SensitivityValue baseValue = new SensitivityValue(0, -1, -1, 1, 2);
        SensitivityValue contingencyValue = new SensitivityValue(0, 0, -1, 3, 4);
        SensitivityState postContingency = SensitivityState.postContingency(contingencyId);
        SensitivityStateStatus.ComponentStatus converged = new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, "");
        SensitivityStateStatus.ComponentStatus failed = new SensitivityStateStatus.ComponentStatus(1, 1, LoadFlowResult.ComponentResult.Status.FAILED, "failed");
        SensitivityStateStatus.ComponentStatus diverged = new SensitivityStateStatus.ComponentStatus(1, 1, LoadFlowResult.ComponentResult.Status.MAX_ITERATION_REACHED, "");
        SensitivityStateStatus.ComponentStatus noCalculation = new SensitivityStateStatus.ComponentStatus(-1, -1, LoadFlowResult.ComponentResult.Status.NO_CALCULATION, "");

        // Only NO_CALCULATION represents a no-impact contingency (for example, an already opened line).
        for (List<SensitivityStateStatus.ComponentStatus> components : List.of(List.of(converged), List.of(converged, converged), List.<SensitivityStateStatus.ComponentStatus>of(),
            List.of(noCalculation), List.of(noCalculation, noCalculation))) {
            SensitivityAnalysisResult input = new SensitivityAnalysisResult(List.of(factor), List.of(new SensitivityStateStatus(postContingency, components)),
                List.of(contingencyId), List.of(), List.of(baseValue, contingencyValue), true);
            SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, outageInstantOrder);
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(contingencyCnec.getState()));
            assertEquals(2, result.getReferenceFlow(nStateCnec, TwoSides.ONE));
            assertEquals(4, result.getReferenceFlow(contingencyCnec, TwoSides.ONE));
            assertEquals(3, result.getSensitivityOnFlow("pst", contingencyCnec, TwoSides.ONE));
        }
        // Mixed NO_CALCULATION statuses and failed or diverged components invalidate the contingency, not the base case.
        for (List<SensitivityStateStatus.ComponentStatus> components : List.of(List.of(converged, failed), List.of(converged, diverged),
            List.of(converged, noCalculation), List.of(noCalculation, failed), List.of(noCalculation, diverged))) {
            SensitivityAnalysisResult input = new SensitivityAnalysisResult(List.of(factor), List.of(new SensitivityStateStatus(postContingency, components)),
                List.of(contingencyId), List.of(), List.of(baseValue, contingencyValue), true);
            SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, outageInstantOrder);
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(contingencyCnec.getState()));
            assertEquals(2, result.getReferenceFlow(nStateCnec, TwoSides.ONE));
            assertTrue(Double.isNaN(result.getReferenceFlow(contingencyCnec, TwoSides.ONE)));
            assertTrue(Double.isNaN(result.getSensitivityOnFlow("pst", contingencyCnec, TwoSides.ONE)));
        }
    }

    @Test
    void testEmptyBaseComponentStatuses() {
        // Empty component-status lists are accepted for both states and do not discard their values.
        setUpWith12Nodes();
        String contingencyId = contingencyCnec.getState().getContingency().orElseThrow().getId();
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, nStateCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.all());
        SensitivityAnalysisResult input = new SensitivityAnalysisResult(List.of(factor), List.of(
            new SensitivityStateStatus(SensitivityState.PRE_CONTINGENCY, List.of()),
            new SensitivityStateStatus(SensitivityState.postContingency(contingencyId), List.of())),
            List.of(contingencyId), List.of(), List.of(new SensitivityValue(0, -1, -1, 1, 2), new SensitivityValue(0, 0, -1, 3, 4)), true);

        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, outageInstantOrder);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(contingencyCnec.getState()));
        assertEquals(2, result.getReferenceFlow(nStateCnec, TwoSides.ONE));
        assertEquals(1, result.getSensitivityOnFlow("pst", nStateCnec, TwoSides.ONE));
        assertEquals(4, result.getReferenceFlow(contingencyCnec, TwoSides.ONE));
        assertEquals(3, result.getSensitivityOnFlow("pst", contingencyCnec, TwoSides.ONE));
    }

    @Test
    void testFailedBaseCaseStatus() {
        // A failed base case makes the overall result fail, even when the contingency converged.
        setUpWith12Nodes();
        String contingencyId = contingencyCnec.getState().getContingency().orElseThrow().getId();
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, nStateCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.all());
        SensitivityValue baseValue = new SensitivityValue(0, -1, -1, 1, 2);
        SensitivityValue contingencyValue = new SensitivityValue(0, 0, -1, 3, 4);
        SensitivityState postContingency = SensitivityState.postContingency(contingencyId);
        SensitivityStateStatus.ComponentStatus converged = new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, "");
        SensitivityStateStatus.ComponentStatus failed = new SensitivityStateStatus.ComponentStatus(1, 1, LoadFlowResult.ComponentResult.Status.FAILED, "failed");
        SensitivityAnalysisResult failedBase = new SensitivityAnalysisResult(List.of(factor),
            List.of(new SensitivityStateStatus(SensitivityState.PRE_CONTINGENCY, List.of(failed)), new SensitivityStateStatus(postContingency, List.of(converged))),
            List.of(contingencyId), List.of(), List.of(baseValue, contingencyValue), true);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(failedBase, outageInstantOrder);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(crac.getPreventiveState()));
        assertTrue(Double.isNaN(result.getReferenceFlow(nStateCnec, TwoSides.ONE)));
        assertTrue(Double.isNaN(result.getSensitivityOnFlow("pst", nStateCnec, TwoSides.ONE)));
    }

    @Test
    void testIncompleteResultIsRejected() {
        // Available values and a converged contingency cannot make an incomplete computation usable.
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, "branch",
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.all());
        SensitivityValue baseValue = new SensitivityValue(0, -1, -1, 1, 2);
        SensitivityValue contingencyValue = new SensitivityValue(0, 0, -1, 3, 4);
        SensitivityState postContingency = SensitivityState.postContingency("contingency");
        SensitivityStateStatus.ComponentStatus converged = new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, "");
        SensitivityAnalysisResult incomplete = new SensitivityAnalysisResult(List.of(factor), List.of(new SensitivityStateStatus(postContingency, List.of(converged))),
            List.of("contingency"), List.of(), List.of(baseValue, contingencyValue), false);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(incomplete, 1);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
        assertTrue(result.getContingencies().isEmpty());
    }

    @Test
    void testNoCalculationBaseStatus() {
        // Unlike a no-impact contingency, NO_CALCULATION in the base case is a failure, alone or mixed with CONVERGED.
        setUpWith12Nodes();
        String contingencyId = contingencyCnec.getState().getContingency().orElseThrow().getId();
        SensitivityFactor factor = new SensitivityFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, nStateCnec.getNetworkElementId(),
            SensitivityVariableType.TRANSFORMER_PHASE, "pst", false, ContingencyContext.all());
        SensitivityStateStatus.ComponentStatus noCalculation = new SensitivityStateStatus.ComponentStatus(-1, -1, LoadFlowResult.ComponentResult.Status.NO_CALCULATION, "");
        SensitivityStateStatus.ComponentStatus converged = new SensitivityStateStatus.ComponentStatus(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, "");
        for (List<SensitivityStateStatus.ComponentStatus> components : List.of(List.of(noCalculation), List.of(converged, noCalculation))) {
            SensitivityAnalysisResult input = new SensitivityAnalysisResult(List.of(factor), List.of(
                new SensitivityStateStatus(SensitivityState.PRE_CONTINGENCY, components),
                new SensitivityStateStatus(SensitivityState.postContingency(contingencyId), List.of(converged))),
                List.of(contingencyId), List.of(), List.of(new SensitivityValue(0, -1, -1, 1, 2)), true);

            SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, outageInstantOrder);
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(crac.getPreventiveState()));
            assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(contingencyCnec.getState()));
            assertTrue(Double.isNaN(result.getReferenceFlow(nStateCnec, TwoSides.ONE)));
            assertTrue(Double.isNaN(result.getSensitivityOnFlow("pst", nStateCnec, TwoSides.ONE)));
        }
    }

    @Test
    void testRecordedContingencyFailureSurvivesLaterSuccessfulCalculation() {
        // Recording a contingency failure before a successful base calculation must still yield a partial failure.
        setUpWith12Nodes();
        SystematicSensitivityResult result = new SystematicSensitivityResult();
        result.recordFailedContingency(contingencyCnec.getState().getContingency().orElseThrow().getId(), outageInstantOrder);
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());

        SensitivityAnalysisResult input = SensitivityAnalysis.find("MockSensi").run(network,
            rangeActionSensitivityProvider.getBasecaseFactors(network), List.of(), new ArrayList<>(), SensitivityAnalysisParameters.load());
        result.completeData(input, crac.getOutageInstant().getOrder());

        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.PARTIAL_FAILURE, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus(contingencyCnec.getState()));
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
    }

}
