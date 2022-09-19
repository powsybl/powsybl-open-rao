/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.importer.CimCracImporter;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Angle monitoring test class
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringTest {
    private static final double ANGLE_TOLERANCE = 0.5;
    private int numberOfLoadFlowsInParallel = 2;

    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private Map<Country, Set<ScalableNetworkElement>> glsks;
    private AngleMonitoringResult angleMonitoringResult;
    private AngleCnec acPrev;
    private AngleCnec acCur1;
    private NetworkAction naOpenL1Prev;
    private NetworkAction naOpenL1Cur;

    @Before
    public void generalSetUp() {
        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);

        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    public void setUpCimCrac(String fileName, OffsetDateTime parametrableOffsetDateTime, CracCreationParameters cracCreationParameters) {
        Properties importParams = new Properties();
        importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        network = Importers.loadNetwork(Paths.get(new File(AngleMonitoringTest.class.getResource("/MicroGrid.zip").getFile()).toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
        InputStream is = getClass().getResourceAsStream(fileName);
        CimCracImporter cracImporter = new CimCracImporter();
        CimCrac cimCrac = cracImporter.importNativeCrac(is);
        CimCracCreator cimCracCreator = new CimCracCreator();
        CimCracCreationContext cracCreationContext = cimCracCreator.createCrac(cimCrac, network, parametrableOffsetDateTime, cracCreationParameters);
        crac = cracCreationContext.getCrac();

        // GLSKs
        Set<ScalableNetworkElement> nLScalable = Set.of(new ScalableNetworkElement("_2844585c-0d35-488d-a449-685bcd57afbf", 10f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("_1dc9afba-23b5-41a0-8540-b479ed8baf4b", 5f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("_9c3b8f97-7972-477d-9dc8-87365cc0ad0e", 40f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("_69add5b4-70bd-4360-8a93-286256c0d38b", 20f, ScalableNetworkElement.ScalableType.LOAD),
                new ScalableNetworkElement("_b1e03a8f-6a11-4454-af58-4a4a680e857f", 20f, ScalableNetworkElement.ScalableType.LOAD),
                new ScalableNetworkElement("_25ab1b5b-6803-47e2-805a-ab7b2072e034", 5f, ScalableNetworkElement.ScalableType.LOAD));

        Set<ScalableNetworkElement> beScalable = Set.of(new ScalableNetworkElement("_550ebe0d-f2b2-48c1-991f-cebea43a21aa", 5f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("_3a3b27be-b18b-4385-b557-6735d733baf0", 20f, ScalableNetworkElement.ScalableType.GENERATOR),
                new ScalableNetworkElement("_1c6beed6-1acf-42e7-ba55-0cc9f04bddd8", 35f, ScalableNetworkElement.ScalableType.LOAD),
                new ScalableNetworkElement("_cb459405-cc14-4215-a45c-416789205904", 30f, ScalableNetworkElement.ScalableType.LOAD),
                new ScalableNetworkElement("_b1480a00-b427-4001-a26c-51954d2bb7e9", 10f, ScalableNetworkElement.ScalableType.LOAD));
        glsks = new HashMap<>();
        glsks.put(Country.BE, beScalable);
        glsks.put(Country.NL, nLScalable);
    }

    public void setUpCracFactory(String networkFileName) {
        network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac");

        Set<ScalableNetworkElement> frScalable = Set.of(new ScalableNetworkElement("G1", 100f, ScalableNetworkElement.ScalableType.GENERATOR));
        glsks = new HashMap<>();
        glsks.put(Country.FR, frScalable);
    }

    public void mockPreventiveState() {
        acPrev = addAngleCnec("acPrev", Instant.PREVENTIVE, null, "VL1", "VL2", -200., 500.);
        naOpenL1Prev = crac.newNetworkAction()
                .withId("Open L1 - 1")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.PREVENTIVE).withAngleCnec(acPrev.getId()).add()
                .add();
    }

    public void mockCurativeStates() {
        crac.newContingency().withId("coL1").withNetworkElement("L1").add();
        crac.newContingency().withId("coL2").withNetworkElement("L2").add();
        crac.newContingency().withId("coL1L2").withNetworkElement("L1").withNetworkElement("L2").add();
        acCur1 = addAngleCnec("acCur1", Instant.CURATIVE, "coL1", "VL1", "VL2", -300., null);
    }

    private AngleCnec addAngleCnec(String id, Instant instant, String contingency, String importingNetworkElement, String exportingNetworkElement, Double min, Double max) {
        return crac.newAngleCnec()
                .withId(id)
                .withInstant(instant)
                .withContingency(contingency)
                .withImportingNetworkElement(importingNetworkElement)
                .withExportingNetworkElement(exportingNetworkElement)
                .withMonitored()
                .newThreshold().withUnit(Unit.DEGREE).withMin(min).withMax(max).add()
                .add();
    }

    private void runAngleMonitoring() {
        angleMonitoringResult = new AngleMonitoring(crac, network, raoResult, glsks, "OpenLoadFlow", loadFlowParameters).run(numberOfLoadFlowsInParallel);
    }

    @Test
    public void testUnknownAngleMonitoring() {
        setUpCracFactory("networkKO.xiidm");
        mockCurativeStates();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnknown());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertTrue(angleMonitoringResult.getAngleCnecsWithAngle().stream().noneMatch(angleResult -> !angleResult.getAngle().isNaN()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("Unknown status on AngleCnecs."));
    }

    @Test
    public void testPreventiveStateOnly() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("AngleCnec acPrev (with importing network element VL1 and exporting network element VL2) at state preventive has an angle of -211°."));
        assertEquals(angleMonitoringResult.getAngle(acPrev, acPrev.getState(), Unit.DEGREE), -211, ANGLE_TOLERANCE);
    }

    @Test
    public void testCurativeStateOnlyWithNoRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -442°."));
    }

    @Test
    public void testCurativeStateOnlyWithAvailableTopoRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naOpenL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        assertEquals(Set.of(naOpenL1Cur.getId()), angleMonitoringResult.getAppliedCras("coL1 - curative"));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -442°."));
    }

    @Test
    public void testCurativeStateOnlyWithAvailableInjectionRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naOpenL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newInjectionSetPoint().withNetworkElement("LD2").withSetpoint(50.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
        assertEquals(Set.of(naOpenL1Cur.getId()), angleMonitoringResult.getAppliedCras("coL1 - curative"));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("All AngleCnecs are secure."));
    }

    @Test
    public void testWrongGlskPercentage() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naOpenL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newInjectionSetPoint().withNetworkElement("LD2").withSetpoint(50.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        Set<ScalableNetworkElement> frScalable = Set.of(new ScalableNetworkElement("G1", 140f, ScalableNetworkElement.ScalableType.GENERATOR));
        glsks = new HashMap<>();
        glsks.put(Country.FR, frScalable);
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnknown());
        assertTrue(angleMonitoringResult.getAppliedCras().isEmpty());
        assertEquals(angleMonitoringResult.printConstraints(), List.of("Unknown status on AngleCnecs."));
    }

    @Test
    public void testWrongGlskCountry() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naOpenL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newInjectionSetPoint().withNetworkElement("LD2").withSetpoint(50.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        Set<ScalableNetworkElement> frScalable = Set.of(new ScalableNetworkElement("G1", 100f, ScalableNetworkElement.ScalableType.GENERATOR));
        glsks = new HashMap<>();
        glsks.put(Country.NL, frScalable);
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnknown());
        assertTrue(angleMonitoringResult.getAppliedCras().isEmpty());
        assertEquals(angleMonitoringResult.printConstraints(), List.of("Unknown status on AngleCnecs."));
    }

    @Test
    public void testCracCim() {
        setUpCimCrac("/CIM_21_7_1.xml", OffsetDateTime.parse("2021-04-02T05:00Z"), new CracCreationParameters());
        assertEquals(2, crac.getAngleCnecs().size());
        assertEquals(Set.of("AngleCnec1", "AngleCnec2"), crac.getAngleCnecs().stream().map(Identifiable::getId).collect(Collectors.toSet()));
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        assertEquals(angleMonitoringResult.printConstraints(), List.of("AngleCnec AngleCnec1 (with importing network element _d77b61ef-61aa-4b22-95f6-b56ca080788d and exporting network element _8d8a82ba-b5b0-4e94-861a-192af055f2b8) at state Co-1 - curative has an angle of 238°."));
    }

    @Test (expected = FaraoException.class)
    public void testGetAngleExceptions1() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        mockCurativeStates();
        double angleValue = angleMonitoringResult.getAngle(acCur1, acCur1.getState(), Unit.DEGREE);
    }

    @Test (expected = FaraoException.class)
    public void testGetAngleExceptions2() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        double angleValue = angleMonitoringResult.getAngle(acPrev, acPrev.getState(), Unit.KILOVOLT);
    }
}
