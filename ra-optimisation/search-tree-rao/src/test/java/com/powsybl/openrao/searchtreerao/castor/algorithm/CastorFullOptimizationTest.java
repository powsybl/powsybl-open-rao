/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.RaoBusinessLogs;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CastorFullOptimizationTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private Network network;
    private State state1;
    private State state2;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private RangeAction<?> ra5;
    private RangeAction<?> ra6;
    private RangeAction<?> ra7;
    private RangeAction<?> ra8;
    private RangeAction<?> ra9;
    private RangeAction<?> ra10;
    private NetworkAction na1;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setup() throws IOException {
        network = Network.read("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network/network_with_alegro_hub.xiidm"));
        crac = Crac.read("small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"), network);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        RaoInput inputs = Mockito.mock(RaoInput.class);
        when(inputs.getNetwork()).thenReturn(network);
        when(inputs.getNetworkVariantId()).thenReturn(network.getVariantManager().getWorkingVariantId());
        when(inputs.getCrac()).thenReturn(crac);
    }

    @Test
    void testShouldRunSecondPreventiveRaoSimple() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        // No SearchTreeRaoParameters extension
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        // Deactivated in parameters
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        // CurativeStopCriterion.MIN_OBJECTIVE
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        // CurativeStopCriterion.SECURE, secure case
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
        Mockito.doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        Mockito.doReturn(0.).when(optimizationResult1).getVirtualCost();
        Mockito.doReturn(0.).when(optimizationResult2).getVirtualCost();
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // CurativeStopCriterion.SECURE, unsecure case 1
        Mockito.doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // CurativeStopCriterion.SECURE, unsecure case 2
        Mockito.doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // CurativeStopCriterion.SECURE, unsecure case 3
        Mockito.doReturn(-10.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(9.).when(optimizationResult1).getVirtualCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0, crac.getInstant(InstantKind.CURATIVE)));
    }

    private void setCost(OptimizationResult optimizationResultMock, double cost) {
        when(optimizationResultMock.getFunctionalCost()).thenReturn(cost);
        when(optimizationResultMock.getVirtualCost()).thenReturn(0.);
        when(optimizationResultMock.getCost()).thenReturn(cost);
    }

    @Test
    void testShouldRunSecondPreventiveRaoAdvanced() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        RaoResult postFirstPreventiveRaoResult = Mockito.mock(RaoResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(10.);
        parameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        when(preventiveResult.getCost()).thenReturn(-500.);

        // CurativeStopCriterion.PREVENTIVE_OBJECTIVE
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE);
        setCost(preventiveResult, -100.);
        // case 1 : final cost is better than preventive (cost < preventive cost - minObjImprovement)
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-200.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // case 2 : final cost = preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-110.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // case 3 : final cost > preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-109.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        // CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        // case 1 : all curatives are better than preventive (cost <= preventive cost - minObjImprovement), SECURE
        setCost(optimizationResult1, -200.);
        setCost(optimizationResult2, -300.);
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-200.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        setCost(optimizationResult1, -110.);
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-110.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // case 2 : all curatives are better than preventive (cost < preventive cost - minObjImprovement), UNSECURE
        setCost(preventiveResult, -500.);
        setCost(optimizationResult1, -200.);
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(0.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        setCost(optimizationResult1, 10.);
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(10.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
        // case 3 : one curative has cost > preventive cost - minObjImprovement, SECURE
        setCost(preventiveResult, -100.);
        setCost(optimizationResult1, -109.);
        when(postFirstPreventiveRaoResult.getCost(curativeInstant)).thenReturn(-109.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
    }

    @Test
    void testShouldRunSecondPreventiveRaoTime() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);

        // Enough time
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 100, crac.getInstant(InstantKind.CURATIVE)));
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 199, crac.getInstant(InstantKind.CURATIVE)));

        // Not enough time
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 201, crac.getInstant(InstantKind.CURATIVE)));
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 400, crac.getInstant(InstantKind.CURATIVE)));
    }

    @Test
    void testShouldRunSecondPreventiveRaoCostIncrease() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE);
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);

        RaoResult postFirstRaoResult = Mockito.mock(RaoResult.class);
        when(postFirstRaoResult.getCost(null)).thenReturn(-100.);
        when(postFirstRaoResult.getCost(preventiveInstant)).thenReturn(-10.);
        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-120.);

        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-100.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));

        when(postFirstRaoResult.getCost(curativeInstant)).thenReturn(-95.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0, crac.getInstant(InstantKind.CURATIVE)));
    }

    private void setUpCracWithRAs() {
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withContingencyElement("contingency1-ne", ContingencyElementType.LINE)
                .add();
        Contingency contingency2 = crac.newContingency()
                .withId("contingency2")
                .withContingencyElement("contingency2-ne", ContingencyElementType.LINE)
                .add();
        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("cnec-ne")
                .withContingency("contingency1")
                .withInstant(CURATIVE_INSTANT_ID)
                .withNominalVoltage(220.)
                .newThreshold().withSide(TwoSides.TWO).withMax(1000.).withUnit(Unit.AMPERE).add()
                .add();
        // ra1 : preventive only
        ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ra1-ne")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra2 : preventive and curative
        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ra2-ne")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNAVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra3 : preventive and curative
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ra3-ne")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withMaxTap(100).withMinTap(-100).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
                .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ra4-ne1")
                .withNetworkElement("ra4-ne2")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = crac.newPstRangeAction()
                .withId("ra5")
                .withNetworkElement("ra4-ne1")
                .withNetworkElement("ra4-ne2")
                .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra6 : preventive and curative (onFlowConstraint)
        ra6 = crac.newPstRangeAction()
                .withId("ra6")
                .withNetworkElement("ra6-ne")
                .withOperator("FR")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnConstraintUsageRule().withCnec("cnec").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra7 : auto only
        ra7 = crac.newPstRangeAction()
                .withId("ra7")
                .withNetworkElement("ra7-ne")
                .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .withSpeed(1)
                .add();
        // ra8 : preventive and auto
        ra8 = crac.newPstRangeAction()
                .withId("ra8")
                .withNetworkElement("ra8-ne")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .withSpeed(2)
                .add();
        // ra9 : preventive only, but with same NetworkElement as ra8
        ra9 = crac.newPstRangeAction()
                .withId("ra9")
                .withNetworkElement("ra8-ne")
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        // ra10 : preventive only, counter trade
        ra10 = crac.newCounterTradeRangeAction()
                .withId("ra10")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
                .newRange().withMin(-1000).withMax(1000).add()
                .add();

        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newSwitchAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency2, curativeInstant);
    }

    @Test
    void testIsRangeActionAvailableInState() {
        setUpCracWithRAs();

        // ra1 is available in preventive only
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra1, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra1, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra1, state2, crac));

        // ra2 is available in state2 only
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra2, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra2, state1, crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra2, state2, crac));

        // ra3 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra3, crac.getPreventiveState(), crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra3, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra3, state2, crac));

        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra4, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra4, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra4, state2, crac));

        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra5, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra5, state1, crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra5, state2, crac));

        // ra6 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, crac.getPreventiveState(), crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra6, state2, crac));

        // ra10 is available in preventive only
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra10, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra10, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra10, state2, crac));
    }

    @Test
    void testIsRangeActionPreventive() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertTrue(CastorFullOptimization.isRangeActionPreventive(ra1, crac));
        // ra2 is available in state2 only
        assertFalse(CastorFullOptimization.isRangeActionPreventive(ra2, crac));
        // ra3 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionPreventive(ra3, crac));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(CastorFullOptimization.isRangeActionPreventive(ra4, crac));
        assertFalse(CastorFullOptimization.isRangeActionPreventive(ra5, crac));
        // ra6 is preventive and curative
        assertTrue(CastorFullOptimization.isRangeActionPreventive(ra6, crac));
    }

    @Test
    void testIsRangeActionCurative() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertFalse(CastorFullOptimization.isRangeActionAutoOrCurative(ra1, crac));
        // ra2 is available in state2 only
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra2, crac));
        // ra3 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra3, crac));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertFalse(CastorFullOptimization.isRangeActionAutoOrCurative(ra4, crac));
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra5, crac));
        // ra6 is preventive and curative
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra6, crac));
    }

    @Test
    void testIsRangeActionAuto() {
        setUpCracWithRAs();
        // ra7 is auto
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra7, crac));
        // ra8 is preventive and auto
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra8, crac));
        // ra9 is preventive with same network element as ra8
        assertFalse(CastorFullOptimization.isRangeActionAutoOrCurative(ra9, crac));
    }

    @Test
    void testGetRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        OptimizationResult firstPreventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        State preventiveState = crac.getPreventiveState();
        // ra9 has different taps than ra8.
        when(firstPreventiveResult.getOptimizedSetpoint(ra9, preventiveState)).thenReturn(2.);
        crac.newRaUsageLimits(autoInstant.getId()).withMaxRa(0).add();
        crac.newRaUsageLimits(curativeInstant.getId()).withMaxRaPerTso(new HashMap<>(Map.of("FR", 0))).add();
        Map<State, OptimizationResult> contingencyResult = new HashMap<>();
        crac.getStates().forEach(state -> {
            if (!state.isPreventive()) {
                contingencyResult.put(state, optimizationResult);
            }
        });

        Set<RangeAction<?>> rangeActionsExcludedFrom2P = CastorFullOptimization.getRangeActionsExcludedFromSecondPreventive(crac, firstPreventiveResult, contingencyResult);

        assertEquals(6, rangeActionsExcludedFrom2P.size());
        assertFalse(rangeActionsExcludedFrom2P.contains(ra1)); // Should not be excluded as it's preventive only.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra2)); // Should be excluded as it's UNAVAILABLE for preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra5)); // Should be excluded as it's not preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra7)); // Should be excluded as it's not preventive.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra3));  // Should be excluded as it has a range limitation RELATIVE_TO_PREVIOUS_INSTANT.

        assertFalse(rangeActionsExcludedFrom2P.contains(ra9)); // It shares the same network elements as ra8 but their tap are different. It should not be excluded.

        assertTrue(rangeActionsExcludedFrom2P.contains(ra6));  // It has the same taps in preventive and in curative. The RA belongs to french TSO and there are ra usage limuts on this TSO : It should be excluded.
        assertTrue(rangeActionsExcludedFrom2P.contains(ra8));  // It has the same taps in preventive and auto. As there are RaUsageLimits for this instant, it should be excluded.
        assertFalse(rangeActionsExcludedFrom2P.contains(ra4)); // It has the same network elements as ra5 and their taps are the same. As it doesn't belong to frenchTSO : it should not be excluded.

    }

    private void setUpCracWithRealRAs(boolean curative) {
        network = NetworkImportsUtil.import12NodesNetwork();
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger();
        HashMap<Integer, Double> tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap.put(stepInt, step.getAlpha()));
        crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withContingencyElement("contingency1-ne", ContingencyElementType.LINE)
                .add();
        Contingency contingency2 = crac.newContingency()
                .withId("contingency2")
                .withContingencyElement("contingency2-ne", ContingencyElementType.LINE)
                .add();
        // ra1 : preventive only
        PstRangeActionAdder adder = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(0).withTapToAngleConversionMap(tapToAngleConversionMap)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        if (curative) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        ra1 = adder.add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
                .withId("na1")
                .newTerminalsConnectionAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency2, curativeInstant);
    }

    @Test
    void testApplyPreventiveResultsForCurativeRangeActions() {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        String pstNeId = "BBE2AA1  BBE3AA1  1";

        setUpCracWithRealRAs(false);
        Mockito.doReturn(-1.5583491325378418).when(optimizationResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(optimizationResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForAutoOrCurativeRangeActions(network, optimizationResult, crac);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());

        setUpCracWithRealRAs(true);
        Mockito.doReturn(-1.5583491325378418).when(optimizationResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(optimizationResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForAutoOrCurativeRangeActions(network, optimizationResult, crac);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    void testGetAppliedRemedialActionsInCurative() {
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);

        String pstNeId = "BBE2AA1  BBE3AA1  1";
        String naNeId = "BBE1AA1  BBE2AA1  1";

        setUpCracWithRealRAs(true);
        Mockito.doReturn(0.).when(prePerimeterResult).getSetpoint(ra1);

        OptimizationResult optimResult1 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(Mockito.any());
        Mockito.doReturn(-1.5583491325378418).when(optimResult1).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of()).when(optimResult1).getActivatedNetworkActions();

        OptimizationResult optimResult2 = Mockito.mock(OptimizationResult.class);
        Mockito.doReturn(Set.of(ra1)).when(optimResult1).getActivatedRangeActions(Mockito.any());
        Mockito.doReturn(0.).when(optimResult2).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(na1)).when(optimResult2).getActivatedNetworkActions();

        Map<State, OptimizationResult> curativeResults = Map.of(state1, optimResult1, state2, optimResult2);

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        CastorFullOptimization.addAppliedNetworkActionsPostContingency(Set.of(autoInstant), appliedRemedialActions, curativeResults);
        CastorFullOptimization.addAppliedNetworkActionsPostContingency(Set.of(curativeInstant), appliedRemedialActions, curativeResults);

        // do not apply network action
        // do not apply range action as it was not yet added to applied RAs
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertTrue(network.getLine(naNeId).getTerminal1().isConnected());

        // reset network
        network = NetworkImportsUtil.import12NodesNetwork();

        // apply only network action
        appliedRemedialActions.applyOnNetwork(state2, network);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());

        // add range action
        CastorFullOptimization.addAppliedRangeActionsPostContingency(Set.of(autoInstant), appliedRemedialActions, curativeResults);
        CastorFullOptimization.addAppliedRangeActionsPostContingency(Set.of(curativeInstant), appliedRemedialActions, curativeResults);

        // apply also range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());
    }

    @Test
    void smallRaoWithDivergingInitialSensi() throws IOException {
        // Small RAO with diverging initial sensi
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)

        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_oneIteration_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
    }

    @Test
    void smallRaoWithout2P() throws IOException {
        // Small RAO without second preventive optimization and only topological actions
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)

        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(493.56, raoResult.getFunctionalCost(preventiveInstant), 1.);
        assertEquals(256.78, raoResult.getFunctionalCost(curativeInstant), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("close_fr1_fr5")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), curativeInstant)));
        assertEquals(FIRST_PREVENTIVE_ONLY, raoResult.getOptimizationStepsExecuted());
    }

    @Test
    void smallRaoWith2P() throws IOException {
        // Same RAO as before but activating 2P => results should be better

        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(preventiveInstant), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(curativeInstant), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), curativeInstant)));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getOptimizationStepsExecuted());
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResult.setOptimizationStepsExecuted(FIRST_PREVENTIVE_ONLY));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
    }

    @Test
    void smallRaoWithGlobal2P() throws IOException {
        // Same RAO as before but activating Global 2P => results should be the same (there are no range actions)

        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate global 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        raoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(preventiveInstant), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(curativeInstant), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), curativeInstant)));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getOptimizationStepsExecuted());
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
    }

    @Test
    void testOptimizationStepsExecutedAndLogsWhenFallbackOnFirstPrev() throws IOException {
        // Catch future logs
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Set up RAO and run
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-2P_cost_increase.json", getClass().getResourceAsStream("/crac/small-crac-2P_cost_increase.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(true);
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // Test Optimization steps executed
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getOptimizationStepsExecuted());
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResult.setOptimizationStepsExecuted(FIRST_PREVENTIVE_ONLY));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());

        // Test final log after RAO fallbacks
        listAppender.stop();
        List<ILoggingEvent> logsList = listAppender.list;
        assert logsList.get(logsList.size() - 1).toString().equals("[INFO] Cost before RAO = 371.88 (functional: 371.88, virtual: 0.00), cost after RAO = 371.88 (functional: 371.88, virtual: 0.00)");
    }

    @Test
    void testThreeCurativeInstantsWithSecondCurativeHavingNoCnecAndNoRa() {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracFactory.findDefault().create("crac");

        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE)
            .newInstant("curative3", InstantKind.CURATIVE);

        Contingency co = crac.newContingency().withId("co1").withContingencyElement("FFR2AA1  FFR3AA1  1", ContingencyElementType.LINE).add();

        crac.newFlowCnec().withId("c1-prev").withInstant("preventive").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2000.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-out").withInstant("auto").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2500.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur1").withInstant("curative1").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2400.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur3").withInstant("curative3").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(1700.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();

        NetworkAction pstPrev = crac.newNetworkAction().withId("pst_fr@10-prev")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(10).add()
            .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        assertEquals(Set.of(pstPrev), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(naCur1), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative1"))));
        assertEquals(Set.of(pstCur), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative3"))));

        FlowCnec cnec;
        cnec = crac.getFlowCnec("c1-prev");
        assertEquals(2228.9, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1979.7, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-228.9, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(20.3, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-out");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(128.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(370.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur1");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(28.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(270.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(850.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur3");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(432.73, raoResult.getFlow(crac.getInstant("curative3"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-671.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(-429.22, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(150.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(1267.27, raoResult.getMargin(crac.getInstant("curative3"), cnec, Unit.AMPERE), 1.);

        assertEquals(671.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(429.22, raoResult.getFunctionalCost(crac.getInstant(InstantKind.PREVENTIVE)), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative1")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative3")), 1.);
    }

    @Test
    void testThreeCurativeInstants() {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracFactory.findDefault().create("crac");

        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative1", InstantKind.CURATIVE)
            .newInstant("curative2", InstantKind.CURATIVE)
            .newInstant("curative3", InstantKind.CURATIVE);

        Contingency co = crac.newContingency().withId("co1").withContingencyElement("FFR2AA1  FFR3AA1  1", ContingencyElementType.LINE).add();

        crac.newFlowCnec().withId("c1-prev").withInstant("preventive").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2000.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-out").withInstant("auto").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2500.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur1").withInstant("curative1").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2400.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur2").withInstant("curative2").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(2300.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();
        crac.newFlowCnec().withId("c1-cur3").withInstant("curative3").withContingency("co1").withNetworkElement("FFR1AA1  FFR4AA1  1").withNominalVoltage(400.)
            .newThreshold().withSide(TwoSides.ONE).withMax(1700.).withUnit(Unit.AMPERE).add()
            .withOptimized().add();

        NetworkAction pstPrev = crac.newNetworkAction().withId("pst_fr@10-prev")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(10).add()
            .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction naCur1 = crac.newNetworkAction().withId("open_fr1_fr3-cur1")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("FFR1AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant("curative1").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur2 = crac.newNetworkAction().withId("pst_fr@-3-cur2")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-3).add()
            .newOnInstantUsageRule().withInstant("curative2").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        NetworkAction pstCur = crac.newNetworkAction().withId("pst_fr@-16-cur3")
            .newPhaseTapChangerTapPositionAction().withNetworkElement("FFR2AA1  FFR4AA1  1").withTapPosition(-16).add()
            .newOnInstantUsageRule().withInstant("curative3").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE);

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        assertEquals(Set.of(pstPrev), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(naCur1), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative1"))));
        assertEquals(Set.of(pstCur2), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative2"))));
        assertEquals(Set.of(pstCur), raoResult.getActivatedNetworkActionsDuringState(crac.getState(co, crac.getInstant("curative3"))));

        FlowCnec cnec;
        cnec = crac.getFlowCnec("c1-prev");
        assertEquals(2228.9, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1979.7, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-228.9, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(20.3, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-out");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(128.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(370.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur1");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(28.12, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(270.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(850.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur2");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(990.32, raoResult.getFlow(crac.getInstant("curative2"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-71.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(170.78, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(750.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(1309.68, raoResult.getMargin(crac.getInstant("curative2"), cnec, Unit.AMPERE), 1.);

        cnec = crac.getFlowCnec("c1-cur3");
        assertEquals(2371.88, raoResult.getFlow(null, cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(2129.22, raoResult.getFlow(crac.getInstant(InstantKind.PREVENTIVE), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(1549.18, raoResult.getFlow(crac.getInstant("curative1"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(990.32, raoResult.getFlow(crac.getInstant("curative2"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(432.73, raoResult.getFlow(crac.getInstant("curative3"), cnec, TwoSides.ONE, Unit.AMPERE), 1.);
        assertEquals(-671.88, raoResult.getMargin(null, cnec, Unit.AMPERE), 1.);
        assertEquals(-429.22, raoResult.getMargin(crac.getInstant(InstantKind.PREVENTIVE), cnec, Unit.AMPERE), 1.);
        assertEquals(150.82, raoResult.getMargin(crac.getInstant("curative1"), cnec, Unit.AMPERE), 1.);
        assertEquals(709.68, raoResult.getMargin(crac.getInstant("curative2"), cnec, Unit.AMPERE), 1.);
        assertEquals(1267.27, raoResult.getMargin(crac.getInstant("curative3"), cnec, Unit.AMPERE), 1.);

        assertEquals(671.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(429.22, raoResult.getFunctionalCost(crac.getInstant(InstantKind.PREVENTIVE)), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative1")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative2")), 1.);
        assertEquals(-20.30, raoResult.getFunctionalCost(crac.getInstant("curative3")), 1.);
    }

    @Test
    void optimizationWithAutoSearchTree() throws IOException {
        network = Network.read("12Nodes_2_twin_lines.uct", getClass().getResourceAsStream("/network/12Nodes_2_twin_lines.uct"));
        crac = Crac.read("small-crac-available-aras.json", getClass().getResourceAsStream("/crac/small-crac-available-aras.json"), network);

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // One FORCED topological ARA is simulated
        // Two AVAILABLE topological ARA are present in the CRAC but one is enough to secure the network
        // One FORCED PST ARA will not be used because the network is already secure after the search tree

        State automatonState = crac.getState("Contingency DE2 NL3 1", crac.getInstant("auto"));
        Set<RangeAction<?>> appliedPstAras = raoResult.getActivatedRangeActionsDuringState(automatonState);

        assertEquals(Set.of("ARA_CLOSE_DE2_NL3_2", "ARA_CLOSE_NL2_BE3_2"), raoResult.getActivatedNetworkActionsDuringState(automatonState).stream().map(NetworkAction::getId).collect(Collectors.toSet()));
        assertTrue(appliedPstAras.isEmpty());

        assertEquals(-382.0, raoResult.getFlow(crac.getInstant("preventive"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - preventive"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-1000.0, raoResult.getFlow(crac.getInstant("outage"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-207.0, raoResult.getFlow(crac.getInstant("auto"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-207.0, raoResult.getFlow(crac.getInstant("curative"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative"), TwoSides.ONE, Unit.MEGAWATT), 1.);
    }

    @Test
    void optimizationWithAutoSearchTreeAndAutoPsts() throws IOException {
        network = Network.read("12Nodes_2_twin_lines.uct", getClass().getResourceAsStream("/network/12Nodes_2_twin_lines.uct"));
        crac = Crac.read("small-crac-available-aras-low-limits-thresholds.json", getClass().getResourceAsStream("/crac/small-crac-available-aras-low-limits-thresholds.json"), network);

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        State automatonState = crac.getState("Contingency DE2 NL3 1", crac.getInstant("auto"));
        List<NetworkAction> appliedNetworkAras = raoResult.getActivatedNetworkActionsDuringState(automatonState).stream().sorted(Comparator.comparing(NetworkAction::getId)).toList();
        Set<RangeAction<?>> appliedPstAras = raoResult.getActivatedRangeActionsDuringState(automatonState);

        assertEquals(3, appliedNetworkAras.size());
        assertEquals("ARA_CLOSE_DE2_NL3_2", appliedNetworkAras.get(0).getId());
        assertEquals("ARA_CLOSE_NL2_BE3_2", appliedNetworkAras.get(1).getId());
        assertEquals("ARA_INJECTION_SETPOINT_800MW", appliedNetworkAras.get(2).getId());
        assertEquals(1, appliedPstAras.size());
        assertEquals("ARA_PST_BE", appliedPstAras.iterator().next().getId());

        assertEquals(-382.0, raoResult.getFlow(crac.getInstant("preventive"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - preventive"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-1000.0, raoResult.getFlow(crac.getInstant("outage"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - outage"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-131.0, raoResult.getFlow(crac.getInstant("auto"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - auto"), TwoSides.ONE, Unit.MEGAWATT), 1.);
        assertEquals(-131.0, raoResult.getFlow(crac.getInstant("curative"), crac.getFlowCnec("NNL2AA1  BBE3AA1  1 - Contingency DE2 NL3 1 - curative"), TwoSides.ONE, Unit.MEGAWATT), 1.);
    }

    @Test
    void threeCurativeInstantsWithCumulativeMaximumNumberOfApplicableRemedialActions() throws IOException {
        network = Network.read("12Nodes_4ParallelLines.uct", getClass().getResourceAsStream("/network/12Nodes_4ParallelLines.uct"));
        crac = Crac.read(
            "small-crac-ra-limits-per-instant.json", CastorFullOptimizationTest.class.getResourceAsStream("/crac/small-crac-ra-limits-per-instant.json"),
            network
        );

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // SearchTree stop criterion is MIN_OBJECTIVE so all 3 remedial actions should be applied during the first curative instant
        // Yet, the number of RAs that can be applied is restricted to 1 (resp. 2) in total for curative1 (resp. curative2)
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative1"))).size());
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative2"))).size());
    }

    @Test
    void threeCurativeInstantsWithCumulativeMaximumNumberOfTsos() throws IOException {
        network = Network.read("12Nodes_4ParallelLines.uct", getClass().getResourceAsStream("/network/12Nodes_4ParallelLines.uct"));
        crac = Crac.read(
            "small-crac-ra-limits-per-instant-3-tsos.json", CastorFullOptimizationTest.class.getResourceAsStream("/crac/small-crac-ra-limits-per-instant-3-tsos.json"),
            network
        );

        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // SearchTree stop criterion is MIN_OBJECTIVE so all 3 remedial actions should be applied during the first curative instant
        // Yet, the number of RAs that can be applied is restricted to 2 (resp. 1) in total for curative1 (resp. curative2)
        assertEquals(2, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative1"))).size());
        assertEquals(0, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative2"))).size());
        assertEquals(1, raoResult.getActivatedNetworkActionsDuringState(crac.getState("contingency", crac.getInstant("curative3"))).size());
    }

    @Test
    void curativeOptimizationShouldNotBeDoneIfPreventiveUnsecure() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-unsecure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-unsecure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Collections.emptySet(), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecure() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-secure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-secure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegative() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-unsecure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-unsecure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(false);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveUnsecureAndAssociatedParameterSet() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-unsecure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-unsecure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveSecureAndAssociatedParameterSet() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-secure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-secure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeOptimizationShouldBeDoneIfPreventiveMinMarginNegativeAndAssociatedParameterSet() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-to-check-curative-optimization-if-preventive-unsecure.json", getClass().getResourceAsStream("/crac/small-crac-to-check-curative-optimization-if-preventive-unsecure.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(Set.of(crac.getNetworkAction("Open FFR1AA1  FFR4AA1  1")), raoResult.getActivatedNetworkActionsDuringState(crac.getState("Contingency FFR2AA1  FFR3AA1  1", crac.getLastInstant())));
    }

    @Test
    void curativeStopCriterionReachedSkipsPerimeterBuilding() throws IOException {
        network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = Crac.read("small-crac-purely-virtual-curative.json", getClass().getResourceAsStream("/crac/small-crac-purely-virtual-curative.json"), network);
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure.json"));

        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(true);

        // Run RAO, if not skipping, then tap to -15, since skipping, it stays at preventive optimization value (-12)
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(-12, raoResult.getOptimizedTapOnState(crac.getState("N-1 NL1-NL3", crac.getLastInstant()), crac.getPstRangeAction("CRA_PST_BE")));
    }

    private State mockState(Instant instant) {
        State state = Mockito.mock(State.class);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }

    private OptimizationResult mockOptimizationResult(Set<NetworkAction> activatedNetworkActions) {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getActivatedNetworkActions()).thenReturn(activatedNetworkActions);
        return optimizationResult;
    }

    private OptimizationResult mockOptimizationResult(Set<RangeAction<?>> activatedRangeActions, State state) {
        OptimizationResult optimizationResult = Mockito.mock(OptimizationResult.class);
        when(optimizationResult.getActivatedRangeActions(state)).thenReturn(activatedRangeActions);
        return optimizationResult;
    }

    @Test
    void testAddAppliedNetworkActionsPostContingency() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        Instant instant1 = Mockito.mock(Instant.class);
        when(instant1.isAuto()).thenReturn(true);
        Instant instant2 = Mockito.mock(Instant.class);
        when(instant2.isCurative()).thenReturn(true);

        State state11 = mockState(instant1);
        State state12 = mockState(instant1);
        State state21 = mockState(instant2);
        State state22 = mockState(instant2);

        NetworkAction na111 = Mockito.mock(NetworkAction.class);
        NetworkAction na112 = Mockito.mock(NetworkAction.class);
        NetworkAction na121 = Mockito.mock(NetworkAction.class);
        NetworkAction na211 = Mockito.mock(NetworkAction.class);
        NetworkAction na221 = Mockito.mock(NetworkAction.class);
        NetworkAction na222 = Mockito.mock(NetworkAction.class);

        OptimizationResult optimizationResult11 = mockOptimizationResult(Set.of(na111, na112));
        OptimizationResult optimizationResult12 = mockOptimizationResult(Set.of(na121));
        OptimizationResult optimizationResult21 = mockOptimizationResult(Set.of(na211));
        OptimizationResult optimizationResult22 = mockOptimizationResult(Set.of(na221, na222));

        Map<State, OptimizationResult> postContingencyResults = Map.of(state11, optimizationResult11, state12, optimizationResult12,
            state21, optimizationResult21, state22, optimizationResult22);

        CastorFullOptimization.addAppliedNetworkActionsPostContingency(Set.of(), appliedRemedialActions, postContingencyResults);

        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state11).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state12).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state22).isEmpty());

        CastorFullOptimization.addAppliedNetworkActionsPostContingency(Set.of(instant1), appliedRemedialActions, postContingencyResults);
        assertEquals(Set.of(na111, na112), appliedRemedialActions.getAppliedNetworkActions(state11));
        assertEquals(Set.of(na121), appliedRemedialActions.getAppliedNetworkActions(state12));
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedNetworkActions(state22).isEmpty());

        CastorFullOptimization.addAppliedNetworkActionsPostContingency(Set.of(instant2), appliedRemedialActions, postContingencyResults);
        assertEquals(Set.of(na111, na112), appliedRemedialActions.getAppliedNetworkActions(state11));
        assertEquals(Set.of(na121), appliedRemedialActions.getAppliedNetworkActions(state12));
        assertEquals(Set.of(na211), appliedRemedialActions.getAppliedNetworkActions(state21));
        assertEquals(Set.of(na221, na222), appliedRemedialActions.getAppliedNetworkActions(state22));
    }

    @Test
    void testAddAppliedRangeActionsPostContingency() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        Instant instant1 = Mockito.mock(Instant.class);
        when(instant1.isAuto()).thenReturn(true);
        Instant instant2 = Mockito.mock(Instant.class);
        when(instant2.isCurative()).thenReturn(true);

        State state11 = mockState(instant1);
        State state12 = mockState(instant1);
        State state21 = mockState(instant2);
        State state22 = mockState(instant2);

        RangeAction<?> ra111 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra112 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra121 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra211 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra221 = Mockito.mock(RangeAction.class);
        RangeAction<?> ra222 = Mockito.mock(RangeAction.class);

        OptimizationResult optimizationResult11 = mockOptimizationResult(Set.of(ra111, ra112), state11);
        OptimizationResult optimizationResult12 = mockOptimizationResult(Set.of(ra121), state12);
        OptimizationResult optimizationResult21 = mockOptimizationResult(Set.of(ra211), state21);
        OptimizationResult optimizationResult22 = mockOptimizationResult(Set.of(ra221, ra222), state22);

        Map<State, OptimizationResult> postContingencyResults = Map.of(state11, optimizationResult11, state12, optimizationResult12,
            state21, optimizationResult21, state22, optimizationResult22);

        CastorFullOptimization.addAppliedRangeActionsPostContingency(Set.of(), appliedRemedialActions, postContingencyResults);

        assertTrue(appliedRemedialActions.getAppliedRangeActions(state11).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state12).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state22).isEmpty());

        CastorFullOptimization.addAppliedRangeActionsPostContingency(Set.of(instant1), appliedRemedialActions, postContingencyResults);
        assertEquals(Map.of(ra111, 0., ra112, 0.), appliedRemedialActions.getAppliedRangeActions(state11));
        assertEquals(Map.of(ra121, 0.), appliedRemedialActions.getAppliedRangeActions(state12));
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state21).isEmpty());
        assertTrue(appliedRemedialActions.getAppliedRangeActions(state22).isEmpty());

        CastorFullOptimization.addAppliedRangeActionsPostContingency(Set.of(instant2), appliedRemedialActions, postContingencyResults);
        assertEquals(Map.of(ra111, 0., ra112, 0.), appliedRemedialActions.getAppliedRangeActions(state11));
        assertEquals(Map.of(ra121, 0.), appliedRemedialActions.getAppliedRangeActions(state12));
        assertEquals(Map.of(ra211, 0.), appliedRemedialActions.getAppliedRangeActions(state21));
        assertEquals(Map.of(ra221, 0., ra222, 0.), appliedRemedialActions.getAppliedRangeActions(state22));
    }

    @Test
    void testIsStopCriterionChecked() throws IOException {
        setup();
        TreeParameters treeParameters = Mockito.mock(TreeParameters.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        // if virtual cost positive return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        assertFalse(CastorFullOptimization.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if purely virtual with null virtual cost, return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-Double.MAX_VALUE);
        assertTrue(CastorFullOptimization.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is MIN_OBJECTIVE return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.MIN_OBJECTIVE);
        assertFalse(CastorFullOptimization.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is higher than target return false
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(-20.);
        assertFalse(CastorFullOptimization.isStopCriterionChecked(objectiveFunctionResult, treeParameters));

        // if not purely virtual and stop criterion is AT_TARGET_OBJECTIVE_VALUE and cost is lower than target return true
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(0.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-10.);
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        assertFalse(CastorFullOptimization.isStopCriterionChecked(objectiveFunctionResult, treeParameters));
    }
}
