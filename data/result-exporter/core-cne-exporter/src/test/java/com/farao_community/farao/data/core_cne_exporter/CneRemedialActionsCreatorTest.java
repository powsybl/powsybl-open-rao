/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.core_cne_exporter.xsd.ConstraintSeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.ContingencySeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.RemedialActionRegisteredResource;
import com.farao_community.farao.data.core_cne_exporter.xsd.RemedialActionSeries;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CneRemedialActionsCreatorTest {

    private Crac crac;
    private Network network;
    private RaoResult raoResult;
    private RaoParameters raoParameters;
    private List<ConstraintSeries> cnecsConstraintSeries;
    private StandardCneExporterParameters exporterParameters;

    @Before
    public void setUp() {
        CneUtil.initUniqueIds();
        network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        exporterParameters = new StandardCneExporterParameters("22XCORESO------S-20211115-F299v1", 10, "10YDOM-REGION-1V", StandardCneExporterParameters.ProcessType.DAY_AHEAD_CC,
            "22XCORESO------S", StandardCneExporterParameters.RoleType.REGIONAL_SECURITY_COORDINATOR, "17XTSO-CS------W", StandardCneExporterParameters.RoleType.CAPACITY_COORDINATOR,
            "2021-10-30T22:00Z/2021-10-31T23:00Z");
        crac = CracFactory.findDefault().create("test-crac");
        crac.newContingency()
                .withId("cnec1")
                .withId("contingency-id")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .add();
        crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withContingency("contingency-id")
                .withInstant(Instant.CURATIVE)
                .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withRule(BranchThresholdRule.ON_NON_REGULATED_SIDE).add()
                .add();
        raoResult = Mockito.mock(RaoResult.class);
        raoParameters = new RaoParameters();

        ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setName("contingency-id");
        contingencySeries.setMRID("contingency-id");

        cnecsConstraintSeries = new ArrayList<>();
        cnecsConstraintSeries.add(new ConstraintSeries());
        cnecsConstraintSeries.get(0).setBusinessType("B88");
        cnecsConstraintSeries.get(0).getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(new ConstraintSeries());
        cnecsConstraintSeries.get(1).setBusinessType("B57");
        cnecsConstraintSeries.get(1).getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(new ConstraintSeries());
        cnecsConstraintSeries.get(2).setBusinessType("B54");
        cnecsConstraintSeries.get(2).getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(new ConstraintSeries());
        cnecsConstraintSeries.get(3).setBusinessType("B54");
    }

    @Test
    public void testPstInitialSetpoint() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("ra-id")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(5)
                .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
                .withOperator("BE")
                .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for PST
        ConstraintSeries constraintSeries = constraintSeriesList.get(0);
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertNull(ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("BBE2AA1  BBE3AA1  1", rs.getName());
        assertEquals(5, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());
    }

    @Test
    public void testPstInitialSetpointUnused() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(false);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(0, constraintSeriesList.size());
    }

    @Test
    public void testIgnorePstWithNoUsageRule() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("ra-id")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(5)
                .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
                .withOperator("BE")
                .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for preventive results shouldn't exist
    }

    @Test
    public void testPstUsedInPreventive() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("ra-id")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(5)
                .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
                .withOperator("BE")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(2, constraintSeriesList.size());

        // B56 for preventive results
        ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        assertTrue(constraintSeries.getContingencySeries().isEmpty());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A18", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("ra-id", rs.getMRID().getValue());
        assertEquals("BBE2AA1  BBE3AA1  1", rs.getName());
        assertEquals(16, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(1, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals("ra-id", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getName());
        assertEquals("A18", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals(1, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54
    }

    @Test
    public void testPstUsedInCurative() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("ra-id")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(5)
                .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
                .withOperator("BE")
                .newOnStateUsageRule().withContingency("contingency-id").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", Instant.OUTAGE))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", Instant.CURATIVE))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", Instant.CURATIVE), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(2, constraintSeriesList.size());

        // B56 for preventive results shouldn't exist

        // B56 for curative results
        ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        assertEquals(1, constraintSeries.getContingencySeries().size());
        assertEquals("contingency-id", constraintSeries.getContingencySeries().get(0).getName());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A19", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("BBE2AA1  BBE3AA1  1", rs.getName());
        assertEquals(16, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());

        // Used PST in curative should be stored in CNECs constraint series B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(0, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals("ra-id", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getName());
        assertEquals("A19", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(0, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54 but with other contingency
    }

    @Test
    public void testIgnoreNetworkActionWithNoUsageRule() {
        NetworkAction networkAction = crac.newNetworkAction()
                .withId("ra-id")
                .newTopologicalAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
                .withOperator("BE")
                .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Set.of(networkAction));

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(0, constraintSeriesList.size());
    }

    @Test
    public void testNetworkActionUsedInPreventive() {
        NetworkAction networkAction = crac.newNetworkAction()
                .withId("ra-id")
                .newTopologicalAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
                .withOperator("BE")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(networkAction));

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for preventive results
        ConstraintSeries constraintSeries = constraintSeriesList.get(0);
        assertTrue(constraintSeries.getContingencySeries().isEmpty());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A18", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertTrue(ra.getRegisteredResource().isEmpty());

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(1, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals("ra-id", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getName());
        assertEquals("A18", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals(1, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54
    }

    @Test
    public void testNetworkActionUsedInCurative() {
        NetworkAction networkAction = crac.newNetworkAction()
                .withId("ra-id")
                .newTopologicalAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
                .withOperator("BE")
                .newOnStateUsageRule().withContingency("contingency-id").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", Instant.OUTAGE))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", Instant.CURATIVE))).thenReturn(Set.of(networkAction));

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for preventive results shouldn't exist

        // B56 for curative results
        ConstraintSeries constraintSeries = constraintSeriesList.get(0);
        assertEquals(1, constraintSeries.getContingencySeries().size());
        assertEquals("contingency-id", constraintSeries.getContingencySeries().get(0).getName());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A19", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertTrue(ra.getRegisteredResource().isEmpty());

        // Used PST in curative should be stored in CNECs constraint series B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(0, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals("ra-id", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getName());
        assertEquals("A19", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(0, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54 but with other contingency
    }

    @Test
    public void testPstInitialSetpointInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext) cneHelper.getCracCreationContext().getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");
        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for PST
        ConstraintSeries constraintSeries = constraintSeriesList.get(0);
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertNull(ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("ra-id", rs.getMRID().getValue());
        assertEquals("BBE3AA1  BBE2AA1  1", rs.getName());
        assertEquals(-5, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());
    }

    @Test
    public void testPstUsedInPreventiveInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext) cneHelper.getCracCreationContext().getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(2, constraintSeriesList.size());

        // B56 for preventive results
        ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        assertTrue(constraintSeries.getContingencySeries().isEmpty());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A18", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10X1001A1001A094", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("BBE3AA1  BBE2AA1  1", rs.getName());
        assertEquals(-16, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(1, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals("ra-id", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getName());
        assertEquals("A18", cnecsConstraintSeries.get(1).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals(1, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54
    }

    @Test
    public void testPstUsedInCurativeInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("FR")
            .newOnStateUsageRule().withContingency("contingency-id").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", Instant.OUTAGE))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", Instant.CURATIVE))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", Instant.CURATIVE), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getState("contingency-id", Instant.CURATIVE), pstRangeAction)).thenReturn(true);

        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CneHelper cneHelper = new CneHelper(crac, network, new MockCracCreationContext(crac), raoResult, raoParameters, exporterParameters);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext) cneHelper.getCracCreationContext().getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");

        CneRemedialActionsCreator cneRemedialActionsCreator = new CneRemedialActionsCreator(cneHelper, cnecsConstraintSeries);

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(2, constraintSeriesList.size());

        // B56 for preventive results shouldn't exist

        // B56 for curative results
        ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        assertEquals(1, constraintSeries.getContingencySeries().size());
        assertEquals("contingency-id", constraintSeries.getContingencySeries().get(0).getName());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        assertEquals("B56", constraintSeries.getBusinessType());
        assertEquals(1, constraintSeries.getRemedialActionSeries().size());
        RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().get(0);
        assertEquals("A19", ra.getApplicationModeMarketObjectStatusStatus());
        assertEquals("ra-id", ra.getName());
        assertEquals(1, ra.getPartyMarketParticipant().size());
        assertEquals("10XFR-RTE------Q", ra.getPartyMarketParticipant().get(0).getMRID().getValue());
        assertEquals(1, ra.getRegisteredResource().size());
        RemedialActionRegisteredResource rs = ra.getRegisteredResource().get(0);
        assertEquals("ra-id", rs.getMRID().getValue());
        assertEquals("BBE3AA1  BBE2AA1  1", rs.getName());
        assertEquals(-16, rs.getResourceCapacityDefaultCapacity().intValue());
        assertEquals("C62", rs.getResourceCapacityUnitSymbol());

        // Used PST in curative should be stored in CNECs constraint series B54
        assertEquals(0, cnecsConstraintSeries.get(0).getRemedialActionSeries().size()); // B88
        assertEquals(0, cnecsConstraintSeries.get(1).getRemedialActionSeries().size()); // B57
        assertEquals(1, cnecsConstraintSeries.get(2).getRemedialActionSeries().size()); // B54
        assertEquals("ra-id", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getName());
        assertEquals("A19", cnecsConstraintSeries.get(2).getRemedialActionSeries().get(0).getApplicationModeMarketObjectStatusStatus());
        assertEquals(0, cnecsConstraintSeries.get(3).getRemedialActionSeries().size()); // B54 but with other contingency
    }

}
