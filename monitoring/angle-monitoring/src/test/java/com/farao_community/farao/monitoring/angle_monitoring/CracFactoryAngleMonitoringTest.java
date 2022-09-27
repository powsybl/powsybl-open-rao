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
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Angle monitoring with Crac Factory test class
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class CracFactoryAngleMonitoringTest {
    private static final double ANGLE_TOLERANCE = 0.5;
    private int numberOfLoadFlowsInParallel = 2;

    private Network network;
    private Crac crac;
    private RaoResult raoResult;
    private LoadFlowParameters loadFlowParameters;
    private CimGlskDocument cimGlskDocument;
    private AngleMonitoringResult angleMonitoringResult;
    private AngleCnec acPrev;
    private AngleCnec acCur1;
    private NetworkAction naOpenL1Prev;
    private NetworkAction naL1Cur;

    @Before
    public void generalSetUp() {
        loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);

        raoResult = Mockito.mock(RaoResult.class);
        when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Collections.emptySet());
        when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Collections.emptySet());
    }

    public void setUpCracFactory(String networkFileName) {
        network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        crac = CracFactory.findDefault().create("test-crac");
        cimGlskDocument = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45test.xml"));
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
        angleMonitoringResult = new AngleMonitoring(crac, network, raoResult, cimGlskDocument, "OpenLoadFlow", loadFlowParameters).run(numberOfLoadFlowsInParallel);
    }

    @Test
    public void testUnknownAngleMonitoring() {
        // LoadFlow diverges
        setUpCracFactory("networkKO.xiidm");
        mockCurativeStates();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnknown());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertTrue(angleMonitoringResult.getAngleCnecsWithAngle().stream().noneMatch(angleResult -> !angleResult.getAngle().isNaN()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("Unknown status on AngleCnecs."));
    }

    @Test
    public void testNoAngleCnecsDefined() {
        setUpCracFactory("network.xiidm");
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
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
        naL1Cur = crac.newNetworkAction()
                .withId("Open L1 - 2")
                .newTopologicalAction().withNetworkElement("L1").withActionType(ActionType.OPEN).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isUnsecure());
        angleMonitoringResult.getAppliedCras().forEach((state, networkActions) -> assertTrue(networkActions.isEmpty()));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("AngleCnec acCur1 (with importing network element VL1 and exporting network element VL2) at state coL1 - curative has an angle of -442°."));
    }

    @Test
    public void testCurativeStateOnlyWithAvailableInjectionRa() {
        setUpCracFactory("network.xiidm");
        mockCurativeStates();
        naL1Cur = crac.newNetworkAction()
                .withId("Injection L1 - 2")
                .newInjectionSetPoint().withNetworkElement("LD2").withSetpoint(50.).add()
                .newOnAngleConstraintUsageRule().withInstant(Instant.CURATIVE).withAngleCnec(acCur1.getId()).add()
                .add();
        runAngleMonitoring();
        assertTrue(angleMonitoringResult.isSecure());
        assertEquals(Set.of(naL1Cur.getId()), angleMonitoringResult.getAppliedCras("coL1 - curative"));
        assertEquals(angleMonitoringResult.printConstraints(), List.of("All AngleCnecs are secure."));
    }

    @Test (expected = FaraoException.class)
    public void testGetAngleExceptions1() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        mockCurativeStates();
        angleMonitoringResult.getAngle(acCur1, acCur1.getState(), Unit.DEGREE);
    }

    @Test (expected = FaraoException.class)
    public void testGetAngleExceptions2() {
        setUpCracFactory("network.xiidm");
        mockPreventiveState();
        runAngleMonitoring();
        angleMonitoringResult.getAngle(acPrev, acPrev.getState(), Unit.KILOVOLT);
    }
}
