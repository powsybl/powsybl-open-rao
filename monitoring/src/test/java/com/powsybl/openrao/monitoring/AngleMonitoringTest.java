/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.monitoring;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.CnecValue;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.io.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;

import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Angle monitoring with Crac Factory test class
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */

class AngleMonitoringTest {
    private static final double ANGLE_TOLERANCE = 0.5;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private MonitoringResult angleMonitoringResult;
    // Crac Factory
    private AngleCnec acPrev;
    private AngleCnec acCur1;
    private NetworkAction naL1Cur;
    private Instant curativeInstant;

    @BeforeEach
    public void generalSetUp() {
        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);
        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    public void setUpCimCrac(String fileName, OffsetDateTime parametrableOffsetDateTime, CracCreationParameters cracCreationParameters) throws IOException {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        network = Network.read(Paths.get(new File(AngleMonitoringTest.class.getResource("/MicroGrid.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        InputStream is = getClass().getResourceAsStream(fileName);
        cracCreationParameters.addExtension(CimCracCreationParameters.class, new CimCracCreationParameters());
        cracCreationParameters.getExtension(CimCracCreationParameters.class).setTimestamp(parametrableOffsetDateTime);
        CimCracCreationContext cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, cracCreationParameters);
        crac = cracCreationContext.getCrac();
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
    }

    public void setUpCracFactory(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
    }

    public void mockPreventiveState() {
        acPrev = addAngleCnec("acPrev", PREVENTIVE_INSTANT_ID, null, "VL1", "VL2", -2., 500.);
        crac.newNetworkAction()
            .withId("Open L1 - 1")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withCnec(acPrev.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
    }

    public void mockCurativeStates() {
        crac.newContingency().withId("coL1").withContingencyElement("L1", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL2").withContingencyElement("L2", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL1L2").withContingencyElement("L1", ContingencyElementType.LINE).withContingencyElement("L2", ContingencyElementType.LINE).add();
        acCur1 = addAngleCnec("acCur1", CURATIVE_INSTANT_ID, "coL1", "VL1", "VL2", -3., null);
    }

    public void mockCurativeStatesSecure() {
        crac.newContingency().withId("coL1").withContingencyElement("L1", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL2").withContingencyElement("L2", ContingencyElementType.LINE).add();
        crac.newContingency().withId("coL1L2").withContingencyElement("L1", ContingencyElementType.LINE).withContingencyElement("L2", ContingencyElementType.LINE).add();
        acCur1 = addAngleCnec("acCur1", CURATIVE_INSTANT_ID, "coL1", "VL1", "VL2", -6., null);
    }

    private AngleCnec addAngleCnec(String id, String instantId, String contingency, String importingNetworkElement, String exportingNetworkElement, Double min, Double max) {
        return crac.newAngleCnec()
            .withId(id)
            .withInstant(instantId)
            .withContingency(contingency)
            .withImportingNetworkElement(importingNetworkElement)
            .withExportingNetworkElement(exportingNetworkElement)
            .withMonitored()
            .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
            .add();
    }

    private void runAngleMonitoring(ZonalData<Scalable> scalableZonalData) {
        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData).build();
        angleMonitoringResult = new Monitoring("OpenLoadFlow", loadFlowParameters).runMonitoring(monitoringInput, 1);
    }

    private RaoResult runAngleMonitoringAndUpdateRaoResult(ZonalData<Scalable> scalableZonalData) {
        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData).build();
        return Monitoring.runAngleAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 1, monitoringInput);
    }

    @Test
    void testDivergentAngleMonitoring() {
        // LoadFlow diverges
        setUpCracFactory("networkKO.xiidm");
        mockCurativeStates();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);
        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.FAILURE, angleMonitoringResult.getStatus());
        angleMonitoringResult.getAppliedRas().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertTrue(angleMonitoringResult.getCnecResults().stream().map(CnecResult::getValue).filter(AngleCnecValue.class::isInstance).allMatch(angleCnecValue -> ((AngleCnecValue) angleCnecValue).value().isNaN()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("ANGLE monitoring failed due to a load flow divergence or an inconsistency in the crac or in the parameters."));
    }

    @Test
    void testNoAngleCnecsDefined() {
        setUpCracFactory("network.xiidm");
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.SECURE, angleMonitoringResult.getStatus());
    }

    @Test
    void testPreventiveStateOnly() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, angleMonitoringResult.getStatus());
        angleMonitoringResult.getAppliedRas().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(List.of("Some ANGLE Cnecs are not secure:",
            "AngleCnec acPrev (with importing network element VL1 and exporting network element VL2) at state preventive has an angle of -3.68°."
        ), angleMonitoringResult.printConstraints());

        double angleValue = angleMonitoringResult.getCnecResults().stream().filter(cr -> cr.getCnec().equals(acPrev)).map(CnecResult::getValue).map(AngleCnecValue.class::cast).findFirst().get().value();
        assertEquals(-3.67, angleValue, ANGLE_TOLERANCE);
    }

    @Test
    void testCurativeStateOnlyWithNoRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, angleMonitoringResult.getStatus());
        angleMonitoringResult.getAppliedRas().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(List.of("Some ANGLE Cnecs are not secure:",
                "AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -7.71°."),
            angleMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStateOnlyWithAvailableTopoRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naL1Cur = crac.newNetworkAction()
            .withId("Open L1 - 2")
            .newTerminalsConnectionAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(acCur1.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.LOW_CONSTRAINT, angleMonitoringResult.getStatus());
        angleMonitoringResult.getAppliedRas().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(List.of("Some ANGLE Cnecs are not secure:",
                "AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -7.71°."),
            angleMonitoringResult.printConstraints());
    }

    @Test
    void testCurativeStateOnlyWithAvailableInjectionRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStatesSecure();
        naL1Cur = crac.newNetworkAction()
            .withId("Injection L1 - 2")
            .newLoadAction().withNetworkElement("LD2").withActivePowerValue(50.).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(acCur1.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.SECURE, angleMonitoringResult.getStatus());
        assertEquals(Set.of(naL1Cur.getId()), angleMonitoringResult.getAppliedRas("coL1 - curative"));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("All ANGLE Cnecs are secure."));
    }

    @Test
    void testAngleCnecOnBus() {
        setUpCracFactory("network.xiidm");
        crac.newContingency().withId("coL1").withContingencyElement("L2", ContingencyElementType.LINE).add();
        acCur1 = addAngleCnec("acCur1", CURATIVE_INSTANT_ID, "coL1", network.getBusView().getBus("VL1_0").getId(), "VL2", -8., null);
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(1, angleMonitoringResult.getCnecResults().size());
        assertEquals("acCur1", angleMonitoringResult.getCnecResults().stream().findFirst().orElseThrow().getCnec().getId());
        assertEquals(Cnec.SecurityStatus.SECURE, angleMonitoringResult.getStatus());
    }

    @Test
    void testCracCim() throws IOException {
        setUpCimCrac("/CIM_21_7_1_AngMon.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        assertEquals(2, crac.getAngleCnecs().size());
        assertEquals(Set.of("AngleCnec1", "AngleCnec2"), crac.getAngleCnecs().stream().map(Identifiable::getId).collect(Collectors.toSet()));
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45MicroGridTest.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);

        assertEquals(Cnec.SecurityStatus.HIGH_CONSTRAINT, angleMonitoringResult.getStatus());

        // Applied cras
        State state = crac.getState("Co-1", curativeInstant);
        assertEquals(1, angleMonitoringResult.getAppliedRas(state).size());
        assertTrue(angleMonitoringResult.getAppliedRas(state).contains(crac.getNetworkAction("RA-1")));
        assertEquals(1, angleMonitoringResult.getAppliedRas("Co-1 - curative").size());
        assertTrue(angleMonitoringResult.getAppliedRas("Co-1 - curative").contains("RA-1"));
        assertEquals(2, angleMonitoringResult.getAppliedRas().size());

        // AngleCnecsWithAngle
        assertEquals(2, angleMonitoringResult.getCnecResults().size());

        double angleValue = angleMonitoringResult.getCnecResults().stream().filter(cr -> cr.getCnec().getId().equals("AngleCnec1")).map(CnecResult::getValue).map(AngleCnecValue.class::cast).findFirst().get().value();
        assertEquals(5.22, angleValue, ANGLE_TOLERANCE);
        assertEquals(List.of("Some ANGLE Cnecs are not secure:",
                "AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5.22°."),
            angleMonitoringResult.printConstraints());
    }

    @Test
    void testCracCimWithRaoResultUpdate() throws IOException {
        setUpCimCrac("/CIM_21_7_1_AngMon.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45MicroGridTest.xml")).getZonalScalable(network);

        RaoResult raoResultWithAngleMonitoring = runAngleMonitoringAndUpdateRaoResult(scalableZonalData);
        // Status checks
        assertFalse(raoResultWithAngleMonitoring.isSecure(PhysicalParameter.ANGLE));
        // Applied cras
        State state = crac.getState("Co-1", curativeInstant);
        assertEquals(1, raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(state).size());
        assertTrue(raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(state).contains(crac.getNetworkAction("RA-1")));
        assertEquals(0, raoResultWithAngleMonitoring.getActivatedRangeActionsDuringState(crac.getState("Co-2", curativeInstant)).size());
        // angle values
        assertEquals(5.22, raoResultWithAngleMonitoring.getAngle(crac.getLastInstant(), crac.getAngleCnec("AngleCnec1"), Unit.DEGREE), ANGLE_TOLERANCE);
        assertEquals(-19.33, raoResultWithAngleMonitoring.getAngle(crac.getLastInstant(), crac.getAngleCnec("AngleCnec2"), Unit.DEGREE), ANGLE_TOLERANCE);
    }

    @Test
    void testAngleMonitoringWithNonValidContingency() {
        setUpCracFactory("network.xiidm");
        crac.newContingency().withId("coL1").withContingencyElement("L1", ContingencyElementType.LINE).add();
        // Type BATTERY is put in purpose to simulate a contingency valid scenario
        crac.newContingency().withId("coL2").withContingencyElement("L2", ContingencyElementType.BATTERY).add();
        acCur1 = addAngleCnec("acCur1", CURATIVE_INSTANT_ID, "coL1", network.getBusView().getBus("VL1_0").getId(), "VL2", -8., null);
        acCur1 = addAngleCnec("acCur2", CURATIVE_INSTANT_ID, "coL2", network.getBusView().getBus("VL1_0").getId(), "VL2", -8., null);

        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        runAngleMonitoring(scalableZonalData);
        assertEquals(Cnec.SecurityStatus.FAILURE, angleMonitoringResult.getStatus());
        assertEquals(2, angleMonitoringResult.getCnecResults().size());

        Optional<CnecResult> acCur1CnecOpt = angleMonitoringResult.getCnecResults().stream().filter(cr -> cr.getId().equals("acCur1")).findFirst();
        CnecValue acCur1CnecValue = acCur1CnecOpt.get().getValue();
        Cnec.SecurityStatus acCur1SecurityStatus = acCur1CnecOpt.get().getCnecSecurityStatus();
        double acCur1Margin = acCur1CnecOpt.get().getMargin();

        assertTrue(acCur1CnecValue instanceof AngleCnecValue);
        assertEquals(-7.71, ((AngleCnecValue) acCur1CnecValue).value(), 0.01);
        assertEquals(Cnec.SecurityStatus.SECURE, acCur1SecurityStatus);
        assertEquals(0.28, acCur1Margin, 0.01);

        Optional<CnecResult> acCur2CnecOpt = angleMonitoringResult.getCnecResults().stream().filter(cr -> cr.getId().equals("acCur2")).findFirst();
        CnecValue acCur2CnecValue = acCur2CnecOpt.get().getValue();
        Cnec.SecurityStatus acCur2SecurityStatus = acCur2CnecOpt.get().getCnecSecurityStatus();
        double acCur2Margin = acCur2CnecOpt.get().getMargin();

        assertTrue(acCur2CnecValue instanceof AngleCnecValue);
        assertEquals(Double.NaN, ((AngleCnecValue) acCur2CnecValue).value(), 0.01);
        assertEquals(Cnec.SecurityStatus.FAILURE, acCur2SecurityStatus);
        assertEquals(Double.NaN, acCur2Margin, 0.01);
    }

    @Test
    void testWithRaoResultUpdate() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        mockCurativeStatesSecure();
        naL1Cur = crac.newNetworkAction()
            .withId("Injection L1 - 2")
            .newLoadAction().withNetworkElement("LD2").withActivePowerValue(50.).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(acCur1.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        ZonalData<Scalable> scalableZonalData = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml")).getZonalScalable(network);

        when(raoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(raoResult.isSecure()).thenReturn(true);

        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).withScalableZonalData(scalableZonalData).build();
        RaoResult raoResultWithAngleMonitoring = Monitoring.runAngleAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2, monitoringInput);

        assertThrows(OpenRaoException.class, () -> raoResultWithAngleMonitoring.getAngle(crac.getPreventiveState().getInstant(), acCur1, Unit.DEGREE));
        assertEquals(2.22, raoResultWithAngleMonitoring.getMargin(crac.getInstant(CURATIVE_INSTANT_ID), acCur1, Unit.DEGREE), 0.01);
        assertEquals(Set.of(naL1Cur), raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(crac.getState("coL1", crac.getInstant(CURATIVE_INSTANT_ID))));
        assertTrue(raoResultWithAngleMonitoring.isActivatedDuringState(crac.getState("coL1", crac.getInstant(CURATIVE_INSTANT_ID)), naL1Cur));
        assertEquals(ComputationStatus.DEFAULT, raoResultWithAngleMonitoring.getComputationStatus());
        assertFalse(raoResultWithAngleMonitoring.isSecure(crac.getInstant(CURATIVE_INSTANT_ID), PhysicalParameter.VOLTAGE));
        assertFalse(raoResultWithAngleMonitoring.isSecure(PhysicalParameter.ANGLE));
        assertFalse(raoResultWithAngleMonitoring.isSecure());
    }

    @Test
    void testNoZonalDataInputForAngleMonitoring() {
        setUpCracFactory("network.xiidm");
        mockCurativeStatesSecure();
        naL1Cur = crac.newNetworkAction()
            .withId("Injection L1 - 2")
            .newLoadAction().withNetworkElement("LD2").withActivePowerValue(50.).add()
            .newOnConstraintUsageRule().withInstant(CURATIVE_INSTANT_ID).withCnec(acCur1.getId()).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        MonitoringInput monitoringInput = new MonitoringInput.MonitoringInputBuilder().withCrac(crac).withNetwork(network).withRaoResult(raoResult).withPhysicalParameter(PhysicalParameter.ANGLE).build();
        angleMonitoringResult = new Monitoring("OpenLoadFlow", loadFlowParameters).runMonitoring(monitoringInput, 2);
        assertEquals(Cnec.SecurityStatus.FAILURE, angleMonitoringResult.getStatus());
    }
}

