/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneHelper;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ConstraintSeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ContingencySeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.CORE_CNE_EXPORT_PROPERTIES_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CoreCneRemedialActionsCreatorTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private RaoResult raoResult;
    private List<ConstraintSeries> cnecsConstraintSeries;
    private Instant outageInstant;
    private Instant curativeInstant;
    private Properties properties;

    @BeforeEach
    public void setUp() {
        CneUtil.initUniqueIds();
        crac = CracFactory.findDefault().create("test-getCrac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        crac.newContingency()
            .withId("cnec1")
            .withId("contingency-id")
            .withContingencyElement("BBE2AA1  BBE3AA1  1", ContingencyElementType.LINE)
            .add();
        // add preventive CNEC for initializing a preventive state
        crac.newFlowCnec()
            .withId("preventiveCnec")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(TwoSides.TWO).add()
            .add();
        crac.newFlowCnec()
            .withId("cnec2")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withContingency("contingency-id")
            .withInstant(CURATIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(100.).withSide(TwoSides.TWO).add()
            .add();
        raoResult = Mockito.mock(RaoResult.class);

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

        properties = new Properties();
        properties.setProperty("rao-result.export.core-cne.relative-positive-margins", "true");
        properties.setProperty("rao-result.export.core-cne.document-id", "22XCORESO------S-20211115-F299v1");
        properties.setProperty("rao-result.export.core-cne.revision-number", "10");
        properties.setProperty("rao-result.export.core-cne.domain-id", "10YDOM-REGION-1V");
        properties.setProperty("rao-result.export.core-cne.process-type", "A48");
        properties.setProperty("rao-result.export.core-cne.sender-id", "22XCORESO------S");
        properties.setProperty("rao-result.export.core-cne.sender-role", "A44");
        properties.setProperty("rao-result.export.core-cne.receiver-id", "17XTSO-CS------W");
        properties.setProperty("rao-result.export.core-cne.receiver-role", "A36");
        properties.setProperty("rao-result.export.core-cne.time-interval", "2021-10-30T22:00Z/2021-10-31T23:00Z");
    }

    @Test
    void testPstInitialSetpoint() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), new ArrayList<>());

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
    void testPstInitialSetpointUnused() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(false);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(0, constraintSeriesList.size());
    }

    @Test
    void testIgnorePstWithNoUsageRule() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(1, constraintSeriesList.size());

        // B56 for preventive results shouldn't exist
    }

    @Test
    void testPstUsedInPreventive() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), cnecsConstraintSeries);

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
    void testPstUsedInCurative() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .newOnContingencyStateUsageRule().withContingency("contingency-id").withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), cnecsConstraintSeries);

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
    void testIgnoreNetworkActionWithNoUsageRule() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("ra-id")
            .newTerminalsConnectionAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Set.of(networkAction));

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), new ArrayList<>());

        List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        assertEquals(0, constraintSeriesList.size());
    }

    @Test
    void testNetworkActionUsedInPreventive() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("ra-id")
            .newTerminalsConnectionAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
            .withOperator("BE")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(networkAction));

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), cnecsConstraintSeries);

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
    void testNetworkActionUsedInCurative() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("ra-id")
            .newTerminalsConnectionAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
            .withOperator("BE")
            .newOnContingencyStateUsageRule().withContingency("contingency-id").withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(networkAction));

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), cnecsConstraintSeries);

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
    void testPstInitialSetpointInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .add();

        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        UcteCracCreationContext cracCreationContext = new MockCracCreationContext(crac);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext)
            cracCreationContext.getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");
        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, new ArrayList<>());

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
    void testPstUsedInPreventiveInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        UcteCracCreationContext cracCreationContext = new MockCracCreationContext(crac);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext)
            cracCreationContext.getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, cnecsConstraintSeries);

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
    void testPstUsedInCurativeInverted() {
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("FR")
            .newOnContingencyStateUsageRule().withContingency("contingency-id").withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(true);

        CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        UcteCracCreationContext cracCreationContext = new MockCracCreationContext(crac);
        MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext)
            cracCreationContext.getRemedialActionCreationContexts().get(0);
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");

        CoreCneRemedialActionsCreator cneRemedialActionsCreator = new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, cnecsConstraintSeries);

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
