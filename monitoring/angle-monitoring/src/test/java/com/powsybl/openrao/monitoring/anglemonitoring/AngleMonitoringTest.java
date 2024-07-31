/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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

    private OffsetDateTime glskOffsetDateTime;
    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private CimGlskDocument cimGlskDocument;
    private AngleMonitoringResult angleMonitoringResult;
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
        CimCracCreationContext cracCreationContext = (CimCracCreationContext) Crac.readWithContext(fileName, is, network, parametrableOffsetDateTime, cracCreationParameters);
        crac = cracCreationContext.getCrac();
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        cimGlskDocument = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45MicroGridTest.xml"));
        glskOffsetDateTime = OffsetDateTime.parse("2021-04-02T05:30Z");
    }

    public void setUpCracFactory(String networkFileName) {
        network = Network.read(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        cimGlskDocument = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml"));
        glskOffsetDateTime = OffsetDateTime.parse("2017-04-12T02:30Z");
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

    private void runAngleMonitoring() {
        angleMonitoringResult = new AngleMonitoring(crac, network, raoResult, cimGlskDocument, glskOffsetDateTime).run("OpenLoadFlow", loadFlowParameters, 2);
    }

    @Test
    void testDivergentAngleMonitoring() {
        // LoadFlow diverges
        setUpCracFactory("networkKO.xiidm");
        mockCurativeStates();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isDivergent());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertTrue(angleMonitoringResult.getAngleCnecsWithAngle().stream().allMatch(angleResult -> angleResult.getAngle().isNaN()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("Load flow divergence."));
    }

    @Test
    void testNoAngleCnecsDefined() {
        setUpCracFactory("network.xiidm");
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
    }

    @Test
    void testPreventiveStateOnly() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of(
            "Some AngleCnecs are not secure:",
            "AngleCnec acPrev (with importing network element VL1 and exporting network element VL2) at state preventive has an angle of -4°."
        ));
        assertEquals(angleMonitoringResult.getAngle(acPrev, Unit.DEGREE), -3.67, ANGLE_TOLERANCE);
    }

    @Test
    void testCurativeStateOnlyWithNoRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of(
            "Some AngleCnecs are not secure:",
            "AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -8°."
        ));
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
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of(
            "Some AngleCnecs are not secure:",
            "AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -8°."
        ));
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
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
        assertEquals(Set.of(naL1Cur.getId()), angleMonitoringResult.getAppliedCras("coL1 - curative"));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("All AngleCnecs are secure."));
    }

    @Test
    void testGetAngleExceptions1() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        mockCurativeStates();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleMonitoringResult.getAngle(acCur1, Unit.DEGREE));
        assertEquals("AngleMonitoringResult was not defined with AngleCnec acCur1 and state coL1 - curative", exception.getMessage());
    }

    @Test
    void testGetAngleExceptions2() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleMonitoringResult.getAngle(acPrev, Unit.KILOVOLT));
        assertEquals("Unhandled unit kV for AngleCnec acPrev", exception.getMessage());
    }

    @Test
    void testAngleCnecOnBus() {
        setUpCracFactory("network.xiidm");
        crac.newContingency().withId("coL1").withContingencyElement("L2", ContingencyElementType.LINE).add();
        acCur1 = addAngleCnec("acCur1", CURATIVE_INSTANT_ID, "coL1", network.getBusView().getBus("VL1_0").getId(), "VL2", -8., null);

        runAngleMonitoring();
        assertEquals(1, angleMonitoringResult.getAngleCnecsWithAngle().size());
        assertEquals("acCur1", angleMonitoringResult.getAngleCnecsWithAngle().stream().findFirst().orElseThrow().getId());
        assertTrue(angleMonitoringResult.isSecure());
    }

    @Test
    void testCracCim() throws IOException {
        setUpCimCrac("/CIM_21_7_1_AngMon.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        assertEquals(2, crac.getAngleCnecs().size());
        assertEquals(Set.of("AngleCnec1", "AngleCnec2"), crac.getAngleCnecs().stream().map(Identifiable::getId).collect(Collectors.toSet()));
        runAngleMonitoring();
        // Status checks
        assertEquals("UNSECURE", angleMonitoringResult.getStatus().toString());
        assertTrue(angleMonitoringResult.isUnsecure());
        assertFalse(angleMonitoringResult.isSecure());
        assertFalse(angleMonitoringResult.isUnknown());
        // Applied cras
        State state = crac.getState("Co-1", curativeInstant);
        assertEquals(1, angleMonitoringResult.getAppliedCras(state).size());
        assertTrue(angleMonitoringResult.getAppliedCras(state).contains(crac.getNetworkAction("RA-1")));
        assertEquals(1, angleMonitoringResult.getAppliedCras("Co-1 - curative").size());
        assertTrue(angleMonitoringResult.getAppliedCras("Co-1 - curative").contains("RA-1"));
        assertEquals(2, angleMonitoringResult.getAppliedCras().size());
        // AngleCnecsWithAngle
        assertEquals(2, angleMonitoringResult.getAngleCnecsWithAngle().size());
        assertEquals(5.22, angleMonitoringResult.getAngle(crac.getAngleCnec("AngleCnec1"), Unit.DEGREE), ANGLE_TOLERANCE);
        assertEquals(angleMonitoringResult.printConstraints(), List.of(
            "Some AngleCnecs are not secure:",
            "AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 5°."
        ));
    }

    @Test
    void testCracCimWithRaoResultUpdate() throws IOException {
        setUpCimCrac("/CIM_21_7_1_AngMon.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        RaoResult raoResultWithAngleMonitoring = new AngleMonitoring(crac, network, raoResult, cimGlskDocument, glskOffsetDateTime).runAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2);
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
    void testCracCimWithProportionalGlsk() throws IOException {
        setUpCimCrac("/CIM_21_7_1_AngMon.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        RaoResult raoResultWithAngleMonitoring = new AngleMonitoring(crac, network, raoResult, Set.of(Country.BE, Country.NL)).runAndUpdateRaoResult("OpenLoadFlow", loadFlowParameters, 2);
        // Status checks
        assertFalse(raoResultWithAngleMonitoring.isSecure(PhysicalParameter.ANGLE));
        // Applied cras
        State state = crac.getState("Co-1", curativeInstant);
        assertEquals(1, raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(state).size());
        assertTrue(raoResultWithAngleMonitoring.getActivatedNetworkActionsDuringState(state).contains(crac.getNetworkAction("RA-1")));
        assertEquals(0, raoResultWithAngleMonitoring.getActivatedRangeActionsDuringState(crac.getState("Co-2", curativeInstant)).size());
        // angle values
        assertEquals(4.53, raoResultWithAngleMonitoring.getAngle(crac.getLastInstant(), crac.getAngleCnec("AngleCnec1"), Unit.DEGREE), ANGLE_TOLERANCE);
        assertEquals(-19.33, raoResultWithAngleMonitoring.getAngle(crac.getLastInstant(), crac.getAngleCnec("AngleCnec2"), Unit.DEGREE), ANGLE_TOLERANCE);

    }
}
