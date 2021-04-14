/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.range_domain.PstRangeImpl;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.usage_rule.OnStateImpl;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerOutput;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.LoopFlowParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MnecParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;


/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class, IteratingLinearOptimizer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class IteratingLinearOptimizerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private IteratingLinearOptimizerInput iteratingLinearOptimizerInput;
    private IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private LinearOptimizer linearOptimizer;
    private ObjectiveFunctionEvaluator costEvaluator;
    private Crac crac;

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);

        costEvaluator = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluator.computeFunctionalCost(any())).thenReturn(0.);
        Mockito.when(costEvaluator.computeVirtualCost(any())).thenReturn(0.);
        List<BranchCnec> cnecList = new ArrayList<>();
        crac.getBranchCnecs().forEach(cnec -> cnecList.add(cnec));
        Mockito.when(costEvaluator.getMostLimitingElements(any(), anyInt())).thenReturn(cnecList);

        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        SystematicSensitivityResult systematicSensitivityResult1 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult2 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult3 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult4 = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult1.getReferenceFlow(Mockito.any())).thenReturn(100.);
        Mockito.when(systematicSensitivityResult2.getReferenceFlow(Mockito.any())).thenReturn(50.);
        Mockito.when(systematicSensitivityResult3.getReferenceFlow(Mockito.any())).thenReturn(20.);
        Mockito.when(systematicSensitivityResult3.getReferenceFlow(Mockito.any())).thenReturn(0.);
        Mockito.when(systematicSensitivityInterface.run(Mockito.any()))
                .thenReturn(systematicSensitivityResult1, systematicSensitivityResult2, systematicSensitivityResult3, systematicSensitivityResult4);

        SensitivityAndLoopflowResults preOptimSensitivityResults = new SensitivityAndLoopflowResults(null, null);

        Map<RangeAction, Double> prePerimeterSetpoints = new HashMap<>();
        prePerimeterSetpoints.put(crac.getRangeAction("PRA_PST_BE"), 0.);

        iteratingLinearOptimizerInput = IteratingLinearOptimizerInput.create()
                .withObjectiveFunctionEvaluator(costEvaluator)
                .withSystematicSensitivityInterface(systematicSensitivityInterface)
                .withPreOptimSensitivityResults(preOptimSensitivityResults)
                .withPreperimeterSetpoints(prePerimeterSetpoints)
                .withNetwork(network)
                .withRangeActions(crac.getRangeActions())
                .withCnecs(crac.getBranchCnecs())
                .withInitialCnecResults(new CnecResults())
                .build();

        RaoParameters raoParameters = new RaoParameters();

        LoopFlowParameters loopFlowParameters = new LoopFlowParameters(
            raoParameters.getLoopFlowApproximationLevel(),
            raoParameters.getLoopFlowAcceptableAugmentation(),
            raoParameters.getLoopFlowViolationCost(),
            raoParameters.getLoopFlowConstraintAdjustmentCoefficient());

        MnecParameters mnecParameters = new MnecParameters(
            raoParameters.getMnecAcceptableMarginDiminution(),
            raoParameters.getMnecViolationCost(),
            raoParameters.getMnecConstraintAdjustmentCoefficient());

        iteratingLinearOptimizerParameters = IteratingLinearOptimizerParameters.create()
                .withLoopFlowParameters(loopFlowParameters)
                .withMnecParameters(mnecParameters)
                .withPtdfSumLowerBound(raoParameters.getPtdfSumLowerBound())
                .withPstPenaltyCost(raoParameters.getPstPenaltyCost())
                .withNegativeMarginObjectiveCoefficient(raoParameters.getNegativeMarginObjectiveCoefficient())
                .withOperatorsNotToOptimize(new HashSet<>())
                .withPstSensitivityThreshold(raoParameters.getPstSensitivityThreshold())
                .withMaxPstPerTso(new HashMap<>())
                .withObjectiveFunction(raoParameters.getObjectiveFunction())
                .withMaxIterations(raoParameters.getMaxIterations()).build();

        linearOptimizer = Mockito.mock(LinearOptimizer.class);
        // TODO: PowerMockito.whenNew(LinearOptimizer.class).withAnyArguments().

        // mock linear optimisation engine
        // linear optimisation returns always the same value -> optimal solution is 1.0 for all RAs
        doAnswer(new Answer() {
            private int count = 1;
            public Object answer(InvocationOnMock invocation) {
                double setPoint;
                switch (count) {
                    case 1:
                        setPoint = 1.0;
                        break;
                    case 2:
                        setPoint = 3.0;
                        break;
                    case 3:
                        setPoint = 3.0;
                        break;
                    default:
                        setPoint = 0;
                        break;
                }
                Map<RangeAction, Double> rangeActionSetpoints = new HashMap<>();
                rangeActionSetpoints.put(crac.getRangeAction("PRA_PST_BE"), setPoint);
                count += 1;

                return new LinearOptimizerOutput(LinearProblem.SolveStatus.OPTIMAL, rangeActionSetpoints, new HashMap<>());
            }
        }).when(linearOptimizer).optimize(any());

    }

    @Test
    public void optimize() {
        Mockito.when(costEvaluator.computeFunctionalCost(Mockito.any())).thenReturn(100., 50., 20., 0.);
        try {
            PowerMockito.whenNew(LinearOptimizer.class).withAnyArguments().thenReturn(linearOptimizer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // run an iterating optimization
        IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = IteratingLinearOptimizer.optimize(iteratingLinearOptimizerInput, iteratingLinearOptimizerParameters);

        // check results
        assertNotNull(iteratingLinearOptimizerOutput);
        assertEquals(LinearProblem.SolveStatus.OPTIMAL, iteratingLinearOptimizerOutput.getSolveStatus());
        assertEquals(20, iteratingLinearOptimizerOutput.getCost(), DOUBLE_TOLERANCE);
        assertEquals(3., iteratingLinearOptimizerOutput.getRangeActionSetpoint(crac.getRangeAction("PRA_PST_BE")), DOUBLE_TOLERANCE);
    }

    @Test
    public void optimizeWithInfeasibility() {
        Mockito.when(costEvaluator.computeFunctionalCost(Mockito.any())).thenReturn(100., 50., 20., 0.);

        Mockito.when(linearOptimizer.optimize(any())).thenReturn(new LinearOptimizerOutput(LinearProblem.SolveStatus.INFEASIBLE, new HashMap<>(), new HashMap<>()));
        try {
            PowerMockito.whenNew(LinearOptimizer.class).withAnyArguments().thenReturn(linearOptimizer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // run an iterating optimization
        IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = IteratingLinearOptimizer.optimize(iteratingLinearOptimizerInput, iteratingLinearOptimizerParameters);

        // check results
        assertNotNull(iteratingLinearOptimizerOutput);
        assertEquals(LinearProblem.SolveStatus.INFEASIBLE, iteratingLinearOptimizerOutput.getSolveStatus());
        assertEquals(100., iteratingLinearOptimizerOutput.getCost(), DOUBLE_TOLERANCE);
        assertEquals(0., iteratingLinearOptimizerOutput.getRangeActionSetpoint(crac.getRangeAction("PRA_PST_BE")), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRemoveRangeActionsIfMaxNumberReached() {
        PstRangeActionImpl rangeActionToRemove = new PstRangeActionImpl("PRA_PST_BE_2", "PRA_PST_BE_2", "BE", new NetworkElement("BBE2AA1  BBE3AA1  1"));
        rangeActionToRemove.addRange(new PstRangeImpl(5, 10, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        rangeActionToRemove.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeActionToRemove);
        Network network = iteratingLinearOptimizerInput.getNetwork();
        crac.desynchronize();
        crac.synchronize(network);

        Map<String, Integer> maxPstPerTso = new HashMap<>();
        maxPstPerTso.put("BE", 1);

        BranchCnec mostLimitingCnec = crac.getBranchCnec("NNL1AA1  NNL2AA1  1");
        SystematicSensitivityResult sensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensitivityResult.getSensitivityOnFlow(crac.getRangeAction("PRA_PST_BE"), mostLimitingCnec)).thenReturn(5.);
        Mockito.when(sensitivityResult.getSensitivityOnFlow(crac.getRangeAction("PRA_PST_BE_2"), mostLimitingCnec)).thenReturn(1.);

        Set<RangeAction> rangeActions = crac.getRangeActions();
        assertEquals(2, rangeActions.size());
        IteratingLinearOptimizer.removeRangeActionsIfMaxNumberReached(rangeActions, maxPstPerTso, mostLimitingCnec, sensitivityResult);

        assertEquals(1, rangeActions.size());
        assertTrue(rangeActions.contains(crac.getRangeAction("PRA_PST_BE")));
        assertFalse(rangeActions.contains(crac.getRangeAction("PRA_PST_BE_2")));
    }

    @Test
    public void testRemoveRangeActionsWithWrongInitialSetpoint() {
        PstRangeActionImpl rangeActionToRemove = new PstRangeActionImpl("PRA_PST_BE_2", "PRA_PST_BE_2", "BE", new NetworkElement("BBE2AA1  BBE3AA1  1"));
        rangeActionToRemove.addRange(new PstRangeImpl(5, 10, RangeType.ABSOLUTE, RangeDefinition.CENTERED_ON_ZERO));
        rangeActionToRemove.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeActionToRemove);
        Network network = iteratingLinearOptimizerInput.getNetwork();
        crac.desynchronize();
        crac.synchronize(network);

        Map<RangeAction, Double> initialSetpoints = new HashMap<>();
        Set<RangeAction> rangeActions = crac.getRangeActions();
        rangeActions.forEach(rangeAction -> initialSetpoints.put(rangeAction, 0.));
        assertEquals(2, rangeActions.size());
        IteratingLinearOptimizer.removeRangeActionsWithWrongInitialSetpoint(rangeActions, initialSetpoints, network);

        assertEquals(1, rangeActions.size());
        assertTrue(rangeActions.contains(crac.getRangeAction("PRA_PST_BE")));
        assertFalse(rangeActions.contains(crac.getRangeAction("PRA_PST_BE_2")));
    }
}

