/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;

    private Network network;
    private FlowCnec nStateCnec;
    private FlowCnec contingencyCnec;
    private RangeAction<?> rangeAction;
    private HvdcRangeAction hvdcRangeAction;
    private SensitivityVariableSet linearGlsk;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;
    private int instantOutageOrder;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(Side.LEFT, Side.RIGHT));
        instantOutageOrder = crac.getInstant("curative").getOrder();

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
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, instantOutageOrder);

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
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            rangeActionSensitivityProvider.getAllFactors(network),
            rangeActionSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, instantOutageOrder).postTreatIntensities();

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
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.find().run(network,
            ptdfSensitivityProvider.getAllFactors(network),
            ptdfSensitivityProvider.getContingencies(network),
            new ArrayList<>(),
            SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, instantOutageOrder).postTreatIntensities();

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
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(sensitivityAnalysisResult, instantOutageOrder).postTreatIntensities();

        // Then
        assertFalse(result.isSuccess());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE, result.getStatus());
    }

    private void setUpForHvdc() {
        Crac crac = CracFactory.findDefault().create("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        crac.newContingency()
            .withId("co")
            .withNetworkElement("NNL2AA11 BBE3AA11 1")
            .add();
        nStateCnec = crac.newFlowCnec()
            .withId("cnec-prev")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstantId("preventive")
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
            .add();
        contingencyCnec = crac.newFlowCnec()
            .withId("cnec-cur")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withContingency("co")
            .withInstantId("outage")
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.RIGHT).add()
            .add();
        hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction()
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
            .completeData(sensitivityAnalysisResult, instantOutageOrder)
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
            .completeData(sensitivityAnalysisResult, instantOutageOrder)
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
