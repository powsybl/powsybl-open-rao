/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.Leaf;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SensitivityComputer.class, SearchTree.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class CastorFullOptimizationTest {
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
    private NetworkAction na1;

    private CastorFullOptimization castorFullOptimization;

    @Before
    public void setup() {
        network = Importers.loadNetwork("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network/network_with_alegro_hub.xiidm"));
        crac = CracImporters.importCrac("crac/small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"));
        RaoInput inputs = Mockito.mock(RaoInput.class);
        when(inputs.getNetwork()).thenReturn(network);
        when(inputs.getNetworkVariantId()).thenReturn(network.getVariantManager().getWorkingVariantId());
        when(inputs.getCrac()).thenReturn(crac);
        RaoParameters raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        java.time.Instant instant = Mockito.mock(java.time.Instant.class);
        castorFullOptimization = new CastorFullOptimization(inputs, raoParameters, instant);
    }

    private void prepareMocks() {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = Mockito.mock(SensitivityComputer.SensitivityComputerBuilder.class);
        when(sensitivityComputerBuilder.withToolProvider(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCnecs(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withRangeActions(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withPtdfsResults(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withPtdfsResults(Mockito.any(), Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCommercialFlowsResults(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCommercialFlowsResults(Mockito.any(), Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withAppliedRemedialActions(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        when(sensitivityComputerBuilder.build()).thenReturn(sensitivityComputer);

        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityResult.getSensitivityStatus(Mockito.any())).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityComputer.getBranchResult(network)).thenReturn(Mockito.mock(FlowResult.class));

        Leaf leaf = Mockito.mock(Leaf.class);
        when(leaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);

        try {
            PowerMockito.whenNew(SensitivityComputer.SensitivityComputerBuilder.class).withNoArguments().thenReturn(sensitivityComputerBuilder);
            PowerMockito.whenNew(Leaf.class).withArguments(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(leaf);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void run() throws ExecutionException, InterruptedException {
        prepareMocks();
        RaoResult raoResult = castorFullOptimization.run().get();
        assertNotNull(raoResult);
    }

    @Test
    public void testShouldRunSecondPreventiveRaoSimple() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        // No SearchTreeRaoParameters extension
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // Deactivated in parameters
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.DISABLED);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // CurativeRaoStopCriterion.MIN_OBJECTIVE
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));

        // CurativeRaoStopCriterion.SECURE, secure case
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE);
        Mockito.doReturn(-1.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(-10.).when(optimizationResult2).getFunctionalCost();
        Mockito.doReturn(0.).when(optimizationResult1).getVirtualCost();
        Mockito.doReturn(0.).when(optimizationResult2).getVirtualCost();
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeRaoStopCriterion.SECURE, unsecure case 1
        Mockito.doReturn(0.).when(optimizationResult1).getFunctionalCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeRaoStopCriterion.SECURE, unsecure case 2
        Mockito.doReturn(5.).when(optimizationResult1).getFunctionalCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
        // CurativeRaoStopCriterion.SECURE, unsecure case 3
        Mockito.doReturn(-10.).when(optimizationResult1).getFunctionalCost();
        Mockito.doReturn(9.).when(optimizationResult1).getVirtualCost();
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, null, 0));
    }

    private void setCost(OptimizationResult optimizationResultMock, double cost) {
        when(optimizationResultMock.getFunctionalCost()).thenReturn(cost);
        when(optimizationResultMock.getVirtualCost()).thenReturn(0.);
        when(optimizationResultMock.getCost()).thenReturn(cost);
    }

    @Test
    public void testShouldRunSecondPreventiveRaoAdvanced() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        RaoResult postFirstPreventiveRaoResult = Mockito.mock(RaoResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoMinObjImprovement(10.);

        // CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE);
        setCost(preventiveResult, -100.);
        // case 1 : final cost is better than preventive (cost < preventive cost - minObjImprovement)
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-200.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 2 : final cost = preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-110.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 3 : final cost > preventive cost - minObjImprovement
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-109.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));

        // CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        // case 1 : all curatives are better than preventive (cost <= preventive cost - minObjImprovement), SECURE
        setCost(optimizationResult1, -200.);
        setCost(optimizationResult2, -300.);
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-200.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        setCost(optimizationResult1, -110.);
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-110.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 2 : all curatives are better than preventive (cost < preventive cost - minObjImprovement), UNSECURE
        setCost(preventiveResult, 1000.);
        setCost(optimizationResult1, 0.);
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(0.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        setCost(optimizationResult1, 10.);
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(10.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
        // case 3 : one curative has cost > preventive cost - minObjImprovement, SECURE
        setCost(preventiveResult, -100.);
        setCost(optimizationResult1, -109.);
        when(postFirstPreventiveRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-109.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstPreventiveRaoResult, null, 0));
    }

    @Test
    public void testShouldRunSecondPreventiveRaoTime() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);

        // Enough time
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 100));
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 199));

        // Not enough time
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 201));
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, null, java.time.Instant.now().plusSeconds(200), 400));
    }

    @Test
    public void testShouldRunSecondPreventiveRaoCostIncrease() {
        RaoParameters parameters = new RaoParameters();
        OptimizationResult preventiveResult = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult1 = Mockito.mock(OptimizationResult.class);
        OptimizationResult optimizationResult2 = Mockito.mock(OptimizationResult.class);
        Collection<OptimizationResult> curativeResults = Set.of(optimizationResult1, optimizationResult2);

        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        searchTreeRaoParameters.setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE);
        searchTreeRaoParameters.setCurativeRaoStopCriterion(SearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE);

        RaoResult postFirstRaoResult = Mockito.mock(RaoResult.class);
        when(postFirstRaoResult.getCost(OptimizationState.INITIAL)).thenReturn(-100.);
        when(postFirstRaoResult.getCost(OptimizationState.AFTER_PRA)).thenReturn(-10.);
        when(postFirstRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-120.);

        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));

        when(postFirstRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-100.);
        assertFalse(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));

        when(postFirstRaoResult.getCost(OptimizationState.AFTER_CRA)).thenReturn(-95.);
        assertTrue(CastorFullOptimization.shouldRunSecondPreventiveRao(parameters, preventiveResult, curativeResults, postFirstRaoResult, null, 0));
    }

    private void setUpCracWithRAs() {
        crac = CracFactory.findDefault().create("test-crac");
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
            .withInstant(Instant.CURATIVE)
            .withNominalVoltage(220.)
            .newThreshold().withSide(Side.RIGHT).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        // ra1 : preventive only
        ra1 = crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("ra1-ne")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.UNDEFINED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra2 : curative only
        ra2 = crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra3 : preventive and curative
        ra3 = crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = crac.newPstRangeAction()
            .withId("ra5")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra6 : preventive and curative (onFlowConstraint)
        ra6 = crac.newPstRangeAction()
            .withId("ra6")
            .withNetworkElement("ra6-ne")
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnFlowConstraintUsageRule().withFlowCnec("cnec").withInstant(Instant.CURATIVE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
            .withId("na1")
            .newTopologicalAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, Instant.CURATIVE);
        state2 = crac.getState(contingency2, Instant.CURATIVE);
    }

    @Test
    public void testIsRangeActionAvailableInState() {
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

        // ra6 is available in preventive and in state1 and in state2
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, crac.getPreventiveState(), crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, state1, crac));
        assertTrue(CastorFullOptimization.isRangeActionAvailableInState(ra6, state2, crac));
    }

    @Test
    public void testIsRangeActionPreventive() {
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
    public void testIsRangeActionCurative() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertFalse(CastorFullOptimization.isRangeActionCurative(ra1, crac));
        // ra2 is available in state2 only
        assertTrue(CastorFullOptimization.isRangeActionCurative(ra2, crac));
        // ra3 is available in preventive and in state1
        assertTrue(CastorFullOptimization.isRangeActionCurative(ra3, crac));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(CastorFullOptimization.isRangeActionCurative(ra4, crac));
        assertTrue(CastorFullOptimization.isRangeActionCurative(ra5, crac));
        // ra6 is preventive and curative
        assertTrue(CastorFullOptimization.isRangeActionCurative(ra6, crac));
    }

    @Test
    public void testGetRangeActionsExcludedFromSecondPreventive() {
        setUpCracWithRAs();
        // detect range actions that are preventive and curative
        Set<RangeAction<?>> rangeActionsExcludedFrom2P = CastorFullOptimization.getRangeActionsExcludedFromSecondPreventive(crac);
        assertEquals(5, rangeActionsExcludedFrom2P.size());
        assertTrue(rangeActionsExcludedFrom2P.contains(ra2));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra3));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra4));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra5));
        assertTrue(rangeActionsExcludedFrom2P.contains(ra6));
    }

    @Test
    public void testRemoveRangeActionsExcludedFromSecondPreventive() {
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
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add();
        if (curative) {
            adder.newOnStateUsageRule().withContingency("contingency1").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add();
        }
        ra1 = adder.add();
        // na1 : preventive + curative
        na1 = crac.newNetworkAction()
            .withId("na1")
            .newTopologicalAction().withNetworkElement("BBE1AA1  BBE2AA1  1").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("contingency2").withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, Instant.CURATIVE);
        state2 = crac.getState(contingency2, Instant.CURATIVE);
    }

    @Test
    public void testApplyPreventiveResultsForCurativeRangeActions() {
        PerimeterResult perimeterResult = Mockito.mock(PerimeterResult.class);
        String pstNeId = "BBE2AA1  BBE3AA1  1";

        setUpCracWithRealRAs(false);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForCurativeRangeActions(network, perimeterResult, crac);
        assertEquals(0, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());

        setUpCracWithRealRAs(true);
        Mockito.doReturn(-1.5583491325378418).when(perimeterResult).getOptimizedSetpoint(eq(ra1), Mockito.any());
        Mockito.doReturn(Set.of(ra1)).when(perimeterResult).getActivatedRangeActions(Mockito.any());
        CastorFullOptimization.applyPreventiveResultsForCurativeRangeActions(network, perimeterResult, crac);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
    }

    @Test
    public void testGetAppliedRemedialActionsInCurative() {
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
        CastorFullOptimization.addAppliedNetworkActionsPostContingency(appliedRemedialActions, curativeResults);

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
        CastorFullOptimization.addAppliedRangeActionsPostContingency(appliedRemedialActions, curativeResults);

        // apply also range action
        appliedRemedialActions.applyOnNetwork(state1, network);
        assertEquals(-4, network.getTwoWindingsTransformer(pstNeId).getPhaseTapChanger().getTapPosition());
        assertFalse(network.getLine(naNeId).getTerminal1().isConnected());
    }

    @Test
    public void smallRaoWithout2P() {
        // Small RAO without second preventive optimization and only topological actions
        // Cannot optimize range actions in unit tests (needs OR-Tools installed)

        Network network = Importers.loadNetwork("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P.json"));

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(OptimizationState.INITIAL), 1.);
        assertEquals(493.56, raoResult.getFunctionalCost(OptimizationState.AFTER_PRA), 1.);
        assertEquals(256.78, raoResult.getFunctionalCost(OptimizationState.AFTER_CRA), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("close_fr1_fr5")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), Instant.CURATIVE)));
    }

    @Test
    public void smallRaoWith2P() {
        // Same RAO as before but activating 2P => results should be better

        Network network = Importers.loadNetwork("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P.json"));

        // Activate 2P
        raoParameters.getExtension(SearchTreeRaoParameters.class).setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(OptimizationState.INITIAL), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(OptimizationState.AFTER_PRA), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(OptimizationState.AFTER_CRA), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), Instant.CURATIVE)));
    }

    @Test
    public void smallRaoWithGlobal2P() {
        // Same RAO as before but activating Global 2P => results should be the same (there are no range actions)

        Network network = Importers.loadNetwork("small-network-2P.uct", getClass().getResourceAsStream("/network/small-network-2P.uct"));
        crac = CracImporters.importCrac("crac/small-crac-2P.json", getClass().getResourceAsStream("/crac/small-crac-2P.json"));
        RaoInput raoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_2P.json"));

        // Activate global 2P
        raoParameters.getExtension(SearchTreeRaoParameters.class).setSecondPreventiveOptimizationCondition(SearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT);
        raoParameters.getExtension(SearchTreeRaoParameters.class).setGlobalOptimizationInSecondPreventive(true);

        // Run RAO
        RaoResult raoResult = new CastorFullOptimization(raoInput, raoParameters, null).run().join();
        assertEquals(371.88, raoResult.getFunctionalCost(OptimizationState.INITIAL), 1.);
        assertEquals(674.6, raoResult.getFunctionalCost(OptimizationState.AFTER_PRA), 1.);
        assertEquals(-555.91, raoResult.getFunctionalCost(OptimizationState.AFTER_CRA), 1.);
        assertEquals(Set.of(crac.getNetworkAction("close_de3_de4"), crac.getNetworkAction("open_fr1_fr2")), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(crac.getNetworkAction("open_fr1_fr3")), raoResult.getActivatedNetworkActionsDuringState(crac.getState(crac.getContingency("co1_fr2_fr3_1"), Instant.CURATIVE)));
    }

}
