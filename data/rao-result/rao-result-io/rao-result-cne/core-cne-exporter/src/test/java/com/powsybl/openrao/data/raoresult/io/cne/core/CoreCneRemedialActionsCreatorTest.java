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
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneHelper;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ConstraintSeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ContingencySeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionRegisteredResource;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.RemedialActionSeries;
import org.assertj.core.api.Assertions;
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
        setUpCrac();
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        raoResult = Mockito.mock(RaoResult.class);
        setUpContingencySeries();
        setUpProperties();
    }

    private void setUpCrac() {
        crac = CracFactory.findDefault().create("test-getCrac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
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
    }

    private void setUpContingencySeries() {
        final ContingencySeries contingencySeries = new ContingencySeries();
        contingencySeries.setName("contingency-id");
        contingencySeries.setMRID("contingency-id");

        cnecsConstraintSeries = new ArrayList<>();
        final ConstraintSeries seriesB88 = new ConstraintSeries();
        seriesB88.setBusinessType("B88");
        seriesB88.getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(seriesB88);
        final ConstraintSeries seriesB57 = new ConstraintSeries();
        seriesB57.setBusinessType("B57");
        seriesB57.getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(seriesB57);
        final ConstraintSeries seriesB54WithContingencies = new ConstraintSeries();
        seriesB54WithContingencies.setBusinessType("B54");
        seriesB54WithContingencies.getContingencySeries().add(contingencySeries);
        cnecsConstraintSeries.add(seriesB54WithContingencies);
        final ConstraintSeries seriesB54WithoutContingencies = new ConstraintSeries();
        seriesB54WithoutContingencies.setBusinessType("B54");
        cnecsConstraintSeries.add(seriesB54WithoutContingencies);
    }

    private void setUpProperties() {
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

    private CoreCneRemedialActionsCreator getRemedialActionsCreator(final List<ConstraintSeries> cnecConstraintSeriesList) {
        final CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        return new CoreCneRemedialActionsCreator(cneHelper, new MockCracCreationContext(crac), cnecConstraintSeriesList);
    }

    private CoreCneRemedialActionsCreator getInvertedRemedialActionCreator(final List<ConstraintSeries> cnecConstraintSeriesList) {
        final CneHelper cneHelper = new CneHelper(crac, raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
        final UcteCracCreationContext cracCreationContext = new MockCracCreationContext(crac);
        final MockCracCreationContext.MockRemedialActionCreationContext raContext = (MockCracCreationContext.MockRemedialActionCreationContext)
            cracCreationContext.getRemedialActionCreationContexts().getFirst();
        raContext.setInverted(true);
        raContext.setNativeNetworkElementId("BBE3AA1  BBE2AA1  1");

        return new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, cnecConstraintSeriesList);
    }

    private PstRangeAction getPstRangeAction(final InstantKind instant) {
        final PstRangeActionAdder adder = crac.newPstRangeAction()
            .withId("ra-id")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(5)
            .withTapToAngleConversionMap(Map.of(5, 5., 6, 6.))
            .withOperator("BE");
        if (instant == InstantKind.PREVENTIVE) {
            adder.newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        } else if (instant == InstantKind.CURATIVE) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency-id").withInstant(CURATIVE_INSTANT_ID).add();
        }
        return adder.add();
    }

    private NetworkAction getNetworkAction(final InstantKind instant) {
        final NetworkActionAdder adder = crac.newNetworkAction()
            .withId("na-id")
            .newTerminalsConnectionAction().withNetworkElement("BBE2AA1  BBE3AA1  1").withActionType(ActionType.CLOSE).add()
            .withOperator("BE");
        if (instant == InstantKind.PREVENTIVE) {
            adder.newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        } else if (instant == InstantKind.CURATIVE) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency-id").withInstant(CURATIVE_INSTANT_ID).add();
        }
        return adder.add();
    }

    private static void checkPstRangeAction(final RemedialActionSeries ra,
                                            final String instantCode,
                                            final String elementName,
                                            final int tap) {
        Assertions.assertThat(ra)
            .hasFieldOrPropertyWithValue("applicationModeMarketObjectStatusStatus", instantCode)
            .hasFieldOrPropertyWithValue("name", "ra-id");
        Assertions.assertThat(ra.getPartyMarketParticipant()).hasSize(1);
        Assertions.assertThat(ra.getPartyMarketParticipant().getFirst().getMRID().getValue()).isEqualTo("10X1001A1001A094");
        Assertions.assertThat(ra.getRegisteredResource()).hasSize(1);
        final RemedialActionRegisteredResource rs = ra.getRegisteredResource().getFirst();
        Assertions.assertThat(rs)
            .hasFieldOrPropertyWithValue("name", elementName)
            .hasFieldOrPropertyWithValue("resourceCapacityUnitSymbol", "C62");
        Assertions.assertThat(rs.getResourceCapacityDefaultCapacity().intValue()).isEqualTo(tap);
    }

    private static void checkNetworkAction(final RemedialActionSeries ra, final String instantCode) {
        Assertions.assertThat(ra.getApplicationModeMarketObjectStatusStatus()).isEqualTo(instantCode);
        Assertions.assertThat(ra.getName()).isEqualTo("na-id");
        Assertions.assertThat(ra.getPartyMarketParticipant()).hasSize(1);
        Assertions.assertThat(ra.getPartyMarketParticipant().getFirst().getMRID().getValue()).isEqualTo("10X1001A1001A094");
        Assertions.assertThat(ra.getRegisteredResource()).isEmpty();
    }

    @Test
    void testPstInitialSetpoint() {
        final PstRangeAction pstRangeAction = getPstRangeAction(null);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(new ArrayList<>());

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        Assertions.assertThat(constraintSeriesList).hasSize(1);

        // B56 for PST pre-optim
        final ConstraintSeries constraintSeries = constraintSeriesList.getFirst();
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(ra, null, "BBE2AA1  BBE3AA1  1", 5);
    }

    @Test
    void testPstInitialSetpointUnused() {
        final PstRangeAction pstRangeAction = getPstRangeAction(null);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(false);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(new ArrayList<>());

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        Assertions.assertThat(constraintSeriesList).isEmpty();
    }

    @Test
    void testIgnoreRemedialActionsWithNoUsageRuleAtPreventiveAndCurative() {
        final PstRangeAction pstRangeAction = getPstRangeAction(null);
        final NetworkAction networkAction = getNetworkAction(null);

        // If there was no filtering of remedial actions that do not have usage rules, we would have three B56 (pre-optim, preventive & curative)
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(any())).thenReturn(Set.of(networkAction));
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), networkAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(new ArrayList<>());

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // There should be only one B56 for pre-optim, but none for preventive or curative
        Assertions.assertThat(constraintSeriesList).hasSize(1);
    }

    @Test
    void testPstUsedInPreventive() {
        final PstRangeAction pstRangeAction = getPstRangeAction(InstantKind.PREVENTIVE);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // The list should contain B56 for pre-optim and preventive
        Assertions.assertThat(constraintSeriesList).hasSize(2);

        // B56 for preventive results
        final ConstraintSeries preventiveConstraintSeries = constraintSeriesList.get(1);
        Assertions.assertThat(preventiveConstraintSeries.getContingencySeries()).isEmpty();
        Assertions.assertThat(preventiveConstraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(preventiveConstraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries preventiveRa = preventiveConstraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(preventiveRa, "A18", "BBE2AA1  BBE3AA1  1", 16);

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).hasSize(1); // B57
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(1).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("ra-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A18");
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).hasSize(1); // B54
    }

    @Test
    void testPstUsedInCurative() {
        final PstRangeAction pstRangeAction = getPstRangeAction(InstantKind.CURATIVE);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // The list should contain B56 for pre-optim and curative
        Assertions.assertThat(constraintSeriesList).hasSize(2);

        // B56 for curative results
        final ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        Assertions.assertThat(constraintSeries.getContingencySeries()).hasSize(1);
        Assertions.assertThat(constraintSeries.getContingencySeries().getFirst().getName()).isEqualTo("contingency-id");
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries curativeRa = constraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(curativeRa, "A19", "BBE2AA1  BBE3AA1  1", 16);

        // Used PST in curative should be stored in CNECs constraint series B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).isEmpty(); // B57
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(2).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("ra-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A19");
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).isEmpty(); // B54 but with other contingency
    }

    @Test
    void testNetworkActionUsedInPreventive() {
        final NetworkAction networkAction = getNetworkAction(InstantKind.PREVENTIVE);
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(Set.of(networkAction));

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // In the case of network action, there is no B56 for pre-optim
        Assertions.assertThat(constraintSeriesList).hasSize(1);

        // B56 for preventive results
        final ConstraintSeries constraintSeries = constraintSeriesList.getFirst();
        Assertions.assertThat(constraintSeries.getContingencySeries()).isEmpty();
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkNetworkAction(ra, "A18");

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).hasSize(1); // B57
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(1).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("na-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A18");
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).hasSize(1); // B54
    }

    @Test
    void testNetworkActionUsedInCurative() {
        final NetworkAction networkAction = getNetworkAction(InstantKind.CURATIVE);
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(networkAction));

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getRemedialActionsCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // In the case of network action, there is no B56 for pre-optim
        // The list should contain B56 for curative only
        Assertions.assertThat(constraintSeriesList).hasSize(1);

        // B56 for curative results
        final ConstraintSeries constraintSeries = constraintSeriesList.getFirst();
        Assertions.assertThat(constraintSeries.getContingencySeries()).hasSize(1);
        Assertions.assertThat(constraintSeries.getContingencySeries().getFirst().getName()).isEqualTo("contingency-id");
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkNetworkAction(ra, "A19");

        // Used PST in curative should be stored in CNECs constraint series B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).isEmpty(); // B57
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(2).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("na-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A19");
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).isEmpty(); // B54 but with other contingency
    }

    @Test
    void testPstInitialSetpointInverted() {
        final PstRangeAction pstRangeAction = getPstRangeAction(null);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getInvertedRemedialActionCreator(new ArrayList<>());

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        Assertions.assertThat(constraintSeriesList).hasSize(1);

        // B56 for PST
        final ConstraintSeries constraintSeries = constraintSeriesList.getFirst();
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(ra, null, "BBE3AA1  BBE2AA1  1", -5);
    }

    @Test
    void testPstUsedInPreventiveInverted() {
        final PstRangeAction pstRangeAction = getPstRangeAction(InstantKind.PREVENTIVE);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(any())).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getStates().iterator().next(), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getInvertedRemedialActionCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        Assertions.assertThat(constraintSeriesList).hasSize(2);

        // B56 for preventive results
        final ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        Assertions.assertThat(constraintSeries.getContingencySeries()).isEmpty();
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);

        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(ra, "A18", "BBE3AA1  BBE2AA1  1", -16);

        // Used PST in preventive should be stored in CNECs constraint series B57 & B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).hasSize(1); // B57
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(1).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("ra-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A18");
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).hasSize(1); // B54
    }

    @Test
    void testPstUsedInCurativeInverted() {
        final PstRangeAction pstRangeAction = getPstRangeAction(InstantKind.CURATIVE);
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState())).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", outageInstant))).thenReturn(new HashSet<>());
        Mockito.when(raoResult.getActivatedRangeActionsDuringState(crac.getState("contingency-id", curativeInstant))).thenReturn(Set.of(pstRangeAction));
        Mockito.when(raoResult.getOptimizedTapOnState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(16);
        Mockito.when(raoResult.isActivatedDuringState(crac.getState("contingency-id", curativeInstant), pstRangeAction)).thenReturn(true);

        final CoreCneRemedialActionsCreator cneRemedialActionsCreator = getInvertedRemedialActionCreator(cnecsConstraintSeries);

        final List<ConstraintSeries> constraintSeriesList = cneRemedialActionsCreator.generate();

        // The list should contain B56 for pre-optim and curative
        Assertions.assertThat(constraintSeriesList).hasSize(2);

        // B56 for curative results
        final ConstraintSeries constraintSeries = constraintSeriesList.get(1);
        Assertions.assertThat(constraintSeries.getContingencySeries()).hasSize(1);
        Assertions.assertThat(constraintSeries.getContingencySeries().getFirst().getName()).isEqualTo("contingency-id");
        Assertions.assertThat(constraintSeries.getBusinessType()).isEqualTo("B56");
        Assertions.assertThat(constraintSeries.getRemedialActionSeries()).hasSize(1);
        final RemedialActionSeries ra = constraintSeries.getRemedialActionSeries().getFirst();
        checkPstRangeAction(ra, "A19", "BBE3AA1  BBE2AA1  1", -16);

        // Used PST in curative should be stored in CNECs constraint series B54
        Assertions.assertThat(cnecsConstraintSeries.get(0).getRemedialActionSeries()).isEmpty(); // B88
        Assertions.assertThat(cnecsConstraintSeries.get(1).getRemedialActionSeries()).isEmpty(); // B57
        Assertions.assertThat(cnecsConstraintSeries.get(2).getRemedialActionSeries()).hasSize(1); // B54
        final RemedialActionSeries remedialActionSeries = cnecsConstraintSeries.get(2).getRemedialActionSeries().getFirst();
        Assertions.assertThat(remedialActionSeries.getName()).isEqualTo("ra-id");
        Assertions.assertThat(remedialActionSeries.getApplicationModeMarketObjectStatusStatus()).isEqualTo("A19");
        Assertions.assertThat(cnecsConstraintSeries.get(3).getRemedialActionSeries()).isEmpty(); // B54 but with other contingency
    }
}
