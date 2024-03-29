/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
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

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;
    private int outageInstantOrder;

    public void setUpWith12Nodes() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT, Side.RIGHT));
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
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(200, result.getReferenceIntensity(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, Side.RIGHT), EPSILON);

        // After postTreating intensities
        result.postTreatIntensities();
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, Side.RIGHT), EPSILON);
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
        assertEquals(10, result.getReferenceFlow(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(25, result.getReferenceIntensity(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec, Side.LEFT), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, Side.RIGHT), EPSILON);
        assertEquals(-30, result.getReferenceIntensity(nStateCnec, Side.RIGHT), EPSILON);
        assertEquals(-0.55, result.getSensitivityOnFlow(rangeAction, nStateCnec, Side.RIGHT), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(205, result.getReferenceIntensity(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(5.5, result.getSensitivityOnFlow(rangeAction, contingencyCnec, Side.RIGHT), EPSILON);
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
        assertEquals(10, result.getReferenceFlow(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec, Side.LEFT), EPSILON);
        assertEquals(-15, result.getReferenceFlow(nStateCnec, Side.RIGHT), EPSILON);
        assertEquals(-0.19, result.getSensitivityOnFlow(linearGlsk, nStateCnec, Side.RIGHT), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, Side.LEFT), EPSILON);
        assertEquals(25, result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(-6.5, result.getSensitivityOnFlow(linearGlsk, contingencyCnec, Side.RIGHT), EPSILON);
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
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        crac.newContingency()
            .withId("co")
            .withNetworkElement("NNL2AA11 BBE3AA11 1")
            .add();
        nStateCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
            .add();
        contingencyCnec = crac.newFlowCnec()
            .withId("cnec-cur")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withContingency("co")
            .withInstant(OUTAGE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.RIGHT).add()
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
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElement().getId(), hvdcRangeAction);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult()
            .completeData(sensitivityAnalysisResult, outageInstantOrder)
            .postTreatIntensities()
            .postTreatHvdcs(network, hvdcs);

        assertEquals(30., result.getReferenceFlow(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, Side.LEFT), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(-7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, Side.RIGHT), EPSILON);
    }

    @Test
    void testPostTreatHvdcInvert() {
        setUpForHvdc();
        Map<String, HvdcRangeAction> hvdcs = Map.of(hvdcRangeAction.getNetworkElement().getId(), hvdcRangeAction);
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

        assertEquals(30., result.getReferenceFlow(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(40., result.getReferenceIntensity(nStateCnec, Side.LEFT), EPSILON);
        assertEquals(-0.34, result.getSensitivityOnFlow(hvdcRangeAction, nStateCnec, Side.LEFT), EPSILON);

        assertEquals(26., result.getReferenceFlow(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(-31., result.getReferenceIntensity(contingencyCnec, Side.RIGHT), EPSILON);
        assertEquals(7.5, result.getSensitivityOnFlow(hvdcRangeAction, contingencyCnec, Side.RIGHT), EPSILON);
    }

}
