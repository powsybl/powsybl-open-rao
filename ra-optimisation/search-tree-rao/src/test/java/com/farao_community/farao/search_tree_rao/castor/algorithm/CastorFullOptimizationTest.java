/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.RaoBusinessLogs;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.SecondPreventiveRaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.PerimeterResult;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.result.impl.FailedRaoResultImpl;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CastorFullOptimizationTest {
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
    private NetworkAction na1;
    private CastorFullOptimization castorFullOptimization;
    private Instant instantPrev;
    private Instant instantOutage;
    private Instant instantAuto;
    private Instant instantCurative;

    @BeforeEach
    public void setup() {
        network = Network.read("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network/network_with_alegro_hub.xiidm"));
        crac = CracImporters.importCrac("crac/small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        instantPrev = crac.getInstant("preventive");
        instantOutage = crac.getInstant("outage");
        instantAuto = crac.getInstant("auto");
        instantCurative = crac.getInstant("curative");
        RaoInput inputs = Mockito.mock(RaoInput.class);
        when(inputs.getNetwork()).thenReturn(network);
        when(inputs.getNetworkVariantId()).thenReturn(network.getVariantManager().getWorkingVariantId());
        when(inputs.getCrac()).thenReturn(crac);
        RaoParameters raoParameters = new RaoParameters();
        java.time.Instant instant = Mockito.mock(java.time.Instant.class);
        castorFullOptimization = new CastorFullOptimization(inputs, raoParameters, instant);
    }

    @Test
    void testShouldRunSecondPreventiveRaoSimple() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        // No SearchTreeRaoParameters extension
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // Deactivated in parameters
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // CurativeStopCriterion.MIN_OBJECTIVE
        parameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // CurativeStopCriterion.SECURE, secure case
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.SECURE);
        Mockito.doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        Mockito.doReturn(0.).when(optimizationResult1).getVirtualCost();
        Mockito.doReturn(0.).when(optimizationResult2).getVirtualCost();
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 1
        Mockito.doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 2
        Mockito.doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeStopCriterion.SECURE, unsecure case 3
        Mockito.doReturn(-10.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(9.).when(optimizationResult1).getVirtualCost();
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
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

        // CurativeStopCriterion.PREVENTIVE_OBJECTIVE
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE);
        setCost(preventiveResult, -100.);
        // case 1 : final cost is better than preventive (cost < preventive cost - minObjImprovement)
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-200.);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 2 : final cost = preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-110.);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 3 : final cost > preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-109.);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));

        // CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
        parameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        // case 1 : all curatives are better than preventive (cost <= preventive cost - minObjImprovement), SECURE
        setCost(optimizationResult1, -200.);
        setCost(optimizationResult2, -300.);
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-200.);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        setCost(optimizationResult1, -110.);
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-110.);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 2 : all curatives are better than preventive (cost < preventive cost - minObjImprovement), UNSECURE
        setCost(preventiveResult, 1000.);
        setCost(optimizationResult1, 0.);
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(0.);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        setCost(optimizationResult1, 10.);
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(10.);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 3 : one curative has cost > preventive cost - minObjImprovement, SECURE
        setCost(preventiveResult, -100.);
        setCost(optimizationResult1, -109.);
        when(postFirstPreventiveRaoResult.getCost(instantCurative)).thenReturn(-109.);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
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
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 100));
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 199));

        // Not enough time
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 201));
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 400));
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
        when(postFirstRaoResult.getCost(instantPrev)).thenReturn(-10.);
        when(postFirstRaoResult.getCost(instantCurative)).thenReturn(-120.);

        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));

        when(postFirstRaoResult.getCost(instantCurative)).thenReturn(-100.);
        assertFalse(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));

        when(postFirstRaoResult.getCost(instantCurative)).thenReturn(-95.);
        assertTrue(castorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));
    }

    private void setUpCracWithRAs() {
        crac = CracFactory.findDefault().create("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("contingency1-ne")
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("contingency2-ne")
            .add();
        crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstantId("curative")
            .withNominalVoltage(220.)
            .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        // ra1 : preventive only
        ra1 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("ra1-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstantId("curative").withUsageMethod(UsageMethod.UNDEFINED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra2 : preventive and curative
        ra2 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra3 : preventive and curative
        ra3 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra5")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra6 : preventive and curative (onFlowConstraint)
        ra6 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra6")
            .withNetworkElement("ra6-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("cnec").withInstantId("curative").add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra7 : auto only
        ra7 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra7")
            .withNetworkElement("ra7-ne")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstantId("auto").withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(1)
            .add();
        // ra8 : preventive and auto
        ra8 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra8")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstantId("auto").withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(2)
            .add();
        // ra9 : preventive only, but with same NetworkElement as ra8
        ra9 = (RangeAction<?>) crac.newPstRangeAction()
            .withId("ra9")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // na1 : preventive + curative
        na1 = (NetworkAction) crac.newNetworkAction()
            .withId("na1")
            .newTopologicalAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, "curative");
        state2 = crac.getState(contingency2, "curative");
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
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra4, state2, crac));

        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra5, crac.getPreventiveState(), crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra5, state1, crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra5, state2, crac));

        // ra6 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, crac.getPreventiveState(), crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, state1, crac));
        assertFalse(CastorFullOptimization.isRangeActionAvailableInState(ra6, state2, crac));
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
        assertTrue(CastorFullOptimization.isRangeActionPreventive(ra5, crac));
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
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra4, crac));
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
        assertTrue(CastorFullOptimization.isRangeActionAutoOrCurative(ra9, crac));
    }

    @Test
    void testGetRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        // detect range actions that are preventive and curative
        Set<RangeAction<?>> rangeActionsExcludedFrom2P = CastorFullOptimization.getRangeActionsExcludedFromSecondPreventive(crac);
        assertEquals(8, rangeActionsExcludedFrom2P.size());
        assertTrue(rangeActionsExcludedFrom2P.contains(ra2));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra3));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra4));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra5));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra6));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra7));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra8));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra9));
    }

    @Test
    void testRemoveRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        Set<RangeAction<?>> rangeActions = new HashSet<>(Set.of(ra1, ra2, ra3, ra4, ra5));
        CastorFullOptimization.removeRangeActionsExcludedFromSecondPreventive(rangeActions, crac);
        assertEquals(1, rangeActions.size());
        assertTrue(rangeActions.contains(ra1));
    }

    private void setUpCracWithRealRAs(boolean curative) {
        network = NetworkImportsUtil.import12NodesNetwork();
        PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger();
        HashMap<Integer, Double> tapToAngleConversionMap = new HashMap<>();
        phaseTapChanger.getAllSteps().forEach((stepInt, step) -> tapToAngleConversionMap.put(stepInt, step.getAlpha()));
        crac = CracFactory.findDefault().create("test-crac");
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("contingency1-ne")
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("contingency2-ne")
            .add();
        // ra1 : preventive only
        PstRangeActionAdder adder = crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(0).withTapToAngleConversionMap(tapToAngleConversionMap)
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add();
        if (curative) {
            adder.newOnContingencyStateUsageRule().withContingency("contingency1").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        ra1 = (RangeAction<?>) adder.add();
        // na1 : preventive + curative
        na1 = (NetworkAction) crac.newNetworkAction()
            .withId("na1")
            .newTopologicalAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstantId("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstantId("curative").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, "curative");
        state2 = crac.getState(contingency2, "curative");
    }

    @Test
    void testApplyPreventiveResultsForCurativeRangeActions() {
        PerimeterResult perimeterResult = Mockito.mock(PerimeterResult.class);
        String pstNeId = "BBE2AA1  BBE3AA1  1";

        setUpCracWithRealRAs(false);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForAutoOrCurativeRangeActions(network, perimeterResult, crac);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());

        setUpCracWithRealRAs(true);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForAutoOrCurativeRangeActions(network, perimeterResult, crac);
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
        CastorFullOptimization.addAppliedNetworkActionsPostContingency(instantAuto, appliedRemedialActions, curativeResults);
        CastorFullOptimization.addAppliedNetworkActionsPostContingency(instantCurative, appliedRemedialActions, curativeResults);

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
        CastorFullOptimization.addAppliedRangeActionsPostContingency(instantAuto, appliedRemedialActions, curativeResults);
        CastorFullOptimization.addAppliedRangeActionsPostContingency(instantCurative, appliedRemedialActions, curativeResults);

        // apply also range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());
    }

    @Test
    void smallRaoWithDivergingInitialSensi() {
        // Small RAO with diverging initial sensi
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_oneIteration_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertTrue(raoResult instanceof FailedRaoResultImpl);
    }

    @Test
    void smallRaoWithout2P() {
        // Small RAO without second preventive optimization and only topological actions
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(493.56, raoResult.getFunctionalCost(instantPrev), 1.);
        assertEquals(256.78, raoResult.getFunctionalCost(instantCurative), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("close_fr1_fr5")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), "curative")));
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, raoResult.getOptimizationStepsExecuted());
    }

    @Test
    void smallRaoWith2P() {
        // Same RAO as before but activating 2P => results should be better

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(instantPrev), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(instantCurative), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), "curative")));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getOptimizationStepsExecuted());
        FaraoException exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("", exception.getMessage());
    }

    @Test
    void smallRaoWithGlobal2P() {
        // Same RAO as before but activating Global 2P => results should be the same (there are no range actions)

        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));

        // Activate global 2P
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        raoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(null), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(instantPrev), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(instantCurative), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), "curative")));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, raoResult.getOptimizationStepsExecuted());
        FaraoException exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testOptimizationStepsExecutedAndLogsWhenFallbackOnFirstPrev() {
        // Catch future logs
        Logger logger = (Logger) LoggerFactory.getLogger(RaoBusinessLogs.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Set up RAO and run
        Network network = Network.read("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P_cost_increase.json"));
        crac.addInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.addInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.addInstant("auto", InstantKind.AUTO, "outage");
        crac.addInstant("curative", InstantKind.CURATIVE, "auto");
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P_v2.json"));
        raoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(true);
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();

        // Test Optimization steps executed
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getOptimizationStepsExecuted());
        FaraoException exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("", exception.getMessage());

        // Test final log after RAO fallbacks
        listAppender.stop();
        List<ILoggingEvent> logsList = listAppender.list;
        assert logsList.get(logsList.size() - 1).toString().equals("[INFO] Cost before RAO = 371.88 (functional: 371.88, virtual: 0.00), cost after RAO = 371.88 (functional: 371.88, virtual: 0.00)");
    }
}
