/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.loopflow_computation.LoopFlowComputationWithXnodeGlskHandler;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.results.*;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_api.parameters.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.*;
import com.farao_community.farao.search_tree_rao.output.FailedRaoOutput;
import com.farao_community.farao.search_tree_rao.output.OneStateOnlyRaoOutput;
import com.farao_community.farao.search_tree_rao.output.PreventiveAndCurativesRaoOutput;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRaoProvider implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoProvider.class);
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_STATE = "PreventiveState";
    private static final String CURATIVE_STATE = "CurativeState";
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    private StateTree stateTree;
    private ToolProvider toolProvider;

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public SearchTreeRaoProvider() {
    }

    // Useful for tests
    SearchTreeRaoProvider(StateTree stateTree) {
        this.stateTree = stateTree;
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        RaoUtil.initData(raoInput, parameters);

        stateTree = new StateTree(raoInput.getCrac(), raoInput.getCrac().getPreventiveState());
        ToolProvider.ToolProviderBuilder toolProviderBuilder = ToolProvider.create()
                .withNetwork(raoInput.getNetwork())
                .withRaoParameters(parameters);
        if (raoInput.getReferenceProgram() != null) {
            toolProviderBuilder.withLoopFlowComputation(
                    raoInput.getReferenceProgram(),
                    raoInput.getGlskProvider(),
                    new LoopFlowComputationWithXnodeGlskHandler(
                            raoInput.getGlskProvider(),
                            raoInput.getReferenceProgram(),
                            raoInput.getCrac().getContingencies(),
                            raoInput.getNetwork()
                    )
            );
        }
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            toolProviderBuilder.withAbsolutePtdfSumsComputation(
                    raoInput.getGlskProvider(),
                    new AbsolutePtdfSumsComputation(
                            raoInput.getGlskProvider(),
                            parameters.getRelativeMarginPtdfBoundaries()
                    )
            );
        }
        this.toolProvider = toolProviderBuilder.build();

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            return optimizeOneStateOnly(raoInput, parameters);
        }

        // compute initial sensitivity on all CNECs
        // this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                raoInput.getCrac().getRangeActions(),
                raoInput.getCrac().getFlowCnecs(),
                toolProvider,
                parameters
        );

        PrePerimeterResult initialOutput;
        try {
            initialOutput = prePerimeterSensitivityAnalysis.run(raoInput.getNetwork());
        } catch (SensitivityAnalysisException e) {
            LOGGER.error("Initial sensitivity analysis failed :", e);
            return CompletableFuture.completedFuture(new FailedRaoOutput());
        }

        // optimize preventive perimeter
        LOGGER.info("Preventive perimeter optimization [start]");

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_STATE);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);

        if (stateTree.getOptimizedStates().size() == 1) {
            return optimizePreventivePerimeter(raoInput, parameters, initialOutput);
        }

        PerimeterResult preventiveResult = optimizePreventivePerimeter(raoInput, parameters, initialOutput).join().getPerimeterResult(OptimizationState.AFTER_PRA, raoInput.getCrac().getPreventiveState());
        LOGGER.info("Preventive perimeter optimization [end]");

        // optimize curative perimeters
        double preventiveOptimalCost = preventiveResult.getCost();
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(parameters.getExtension(SearchTreeRaoParameters.class), preventiveOptimalCost);
        applyRemedialActions(raoInput.getNetwork(), preventiveResult);

        PrePerimeterResult preCurativeSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOn(raoInput.getNetwork(), preventiveResult);

        Map<State, OptimizationResult> curativeResults = optimizeCurativePerimeters(raoInput, parameters, curativeTreeParameters, network, initialOutput, preCurativeSensitivityAnalysisOutput);

        // merge variants
        LOGGER.info("Merging preventive and curative RAO results.");
        RaoResult mergedRaoResults = mergeRaoResults(initialOutput, preventiveResult, preCurativeSensitivityAnalysisOutput, curativeResults);

        // log results
        // TODO: find a way to display merged limiting elements from a RaoResult
        return CompletableFuture.completedFuture(mergedRaoResults);
    }

    private void applyRemedialActions(Network network, PerimeterResult perimeterResult) {
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        perimeterResult.getActivatedRangeActions().forEach(rangeAction -> rangeAction.apply(network, perimeterResult.getOptimizedSetPoint(rangeAction)));
    }

    private static LinearOptimizerParameters.LinearOptimizerParametersBuilder basicLinearOptimizerBuilder(RaoParameters raoParameters) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = LinearOptimizerParameters.create()
                .withObjectiveFunction(raoParameters.getObjectiveFunction())
                .withPstSensitivityThreshold(raoParameters.getPstSensitivityThreshold());
        if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE
                || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT) {
            builder.withMaxMinMarginParameters(new MaxMinMarginParameters(raoParameters.getPstPenaltyCost()));
        } else if (raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE
                || raoParameters.getObjectiveFunction() == RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT) {
            MaxMinRelativeMarginParameters maxMinRelativeMarginParameters = new MaxMinRelativeMarginParameters(
                    raoParameters.getPstPenaltyCost(),
                    raoParameters.getNegativeMarginObjectiveCoefficient(),
                    raoParameters.getPtdfSumLowerBound());
            builder.withMaxMinRelativeMarginParameters(maxMinRelativeMarginParameters);
        } else {
            throw new FaraoException(String.format("Unhandled objective function %s", raoParameters.getObjectiveFunction()));
        }

        if (raoParameters.getMnecViolationCost() != 0) {
            MnecParameters mnecParameters = new MnecParameters(
                    raoParameters.getMnecAcceptableMarginDiminution(),
                    raoParameters.getMnecViolationCost(),
                    raoParameters.getMnecConstraintAdjustmentCoefficient());
            builder.withMnecParameters(mnecParameters);
        }

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LoopFlowParameters loopFlowParameters = new LoopFlowParameters(
                    raoParameters.getLoopFlowApproximationLevel(),
                    raoParameters.getLoopFlowAcceptableAugmentation(),
                    raoParameters.getLoopFlowViolationCost(),
                    raoParameters.getLoopFlowConstraintAdjustmentCoefficient());
            builder.withLoopFlowParameters(loopFlowParameters);
        }
        return builder;
    }

    static LinearOptimizerParameters createPreventiveLinearOptimizerParameters(RaoParameters raoParameters) {
        return basicLinearOptimizerBuilder(raoParameters).build();
    }

    static LinearOptimizerParameters createCurativeLinearOptimizerParameters(RaoParameters raoParameters, StateTree stateTree, Set<FlowCnec> cnecs) {
        LinearOptimizerParameters.LinearOptimizerParametersBuilder builder = basicLinearOptimizerBuilder(raoParameters);
        SearchTreeRaoParameters parameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (parameters != null && !parameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            UnoptimizedCnecParameters unoptimizedCnecParameters = new UnoptimizedCnecParameters(
                    stateTree.getOperatorsNotSharingCras(),
                    getLargestCnecThreshold(cnecs));
            builder.withUnoptimizedCnecParameters(unoptimizedCnecParameters);
        }
        return builder.build();
    }

    static double getLargestCnecThreshold(Set<FlowCnec> flowCnecs) {
        double max = 0;
        for (FlowCnec flowCnec : flowCnecs) {
            if (flowCnec.isOptimized()) {
                Optional<Double> minFlow = flowCnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = flowCnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }

    CompletableFuture<RaoResult> optimizeOneStateOnly(RaoInput raoInput, RaoParameters raoParameters) {
        Set<FlowCnec> perimeterCnecs = computePerimeterCnecs(raoInput.getCrac(), raoInput.getPerimeter());
        TreeParameters treeParameters = raoInput.getOptimizedState().equals(raoInput.getCrac().getPreventiveState()) ?
                TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class)) :
                TreeParameters.buildForCurativePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class), -Double.MAX_VALUE);
        LinearOptimizerParameters linearOptimizerParameters = createCurativeLinearOptimizerParameters(raoParameters, stateTree, perimeterCnecs);

        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                raoInput.getCrac().getRangeActions(raoInput.getOptimizedState(), UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED),
                perimeterCnecs,
                toolProvider,
                raoParameters
        );
        PrePerimeterResult prePerimeterResult = prePerimeterSensitivityAnalysis.run(raoInput.getNetwork());

        SearchTreeInput searchTreeInput = buildSearchTreeInput(
                raoInput.getCrac(),
                raoInput.getNetwork(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                prePerimeterResult,
                prePerimeterResult,
                treeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider
        );

        OptimizationResult optimizationResult = new SearchTree().run(searchTreeInput, treeParameters, linearOptimizerParameters).join();

        optimizationResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), optimizationResult.getOptimizedSetPoint(rangeAction)));
        optimizationResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoOutput(raoInput.getOptimizedState(), prePerimeterResult, optimizationResult));
    }

    private CompletableFuture<RaoResult> optimizePreventivePerimeter(RaoInput raoInput, RaoParameters raoParameters, PrePerimeterResult prePerimeterResult) {
        TreeParameters preventiveTreeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        LinearOptimizerParameters linearOptimizerParameters = createPreventiveLinearOptimizerParameters(raoParameters);
        SearchTreeInput searchTreeInput = buildSearchTreeInput(
                raoInput.getCrac(),
                raoInput.getNetwork(),
                raoInput.getCrac().getPreventiveState(),
                stateTree.getPerimeter(raoInput.getCrac().getPreventiveState()),
                prePerimeterResult,
                prePerimeterResult,
                preventiveTreeParameters,
                raoParameters,
                linearOptimizerParameters,
                toolProvider
        );

        OptimizationResult perimeterResult = new SearchTree().run(searchTreeInput, preventiveTreeParameters, linearOptimizerParameters).join();

        perimeterResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(raoInput.getNetwork(), perimeterResult.getOptimizedSetPoint(rangeAction)));
        perimeterResult.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(raoInput.getNetwork()));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoOutput(raoInput.getCrac().getPreventiveState(), prePerimeterResult, perimeterResult));
    }

    private Map<State, OptimizationResult> optimizeCurativePerimeters(RaoInput raoInput,
                                                                      RaoParameters raoParameters,
                                                                      TreeParameters curativeTreeParameters,
                                                                      Network network,
                                                                      PrePerimeterResult initialSensitivityOutput,
                                                                      PrePerimeterResult prePerimeterSensitivityOutput) {
        Map<State, OptimizationResult> curativeResults = new ConcurrentHashMap<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_STATE);
        network.getVariantManager().cloneVariant(PREVENTIVE_STATE, CURATIVE_STATE);
        network.getVariantManager().setWorkingVariant(CURATIVE_STATE);
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_STATE, raoParameters.getPerimetersInParallel())) {
            stateTree.getOptimizedStates().forEach(optimizedState -> {
                if (!optimizedState.equals(raoInput.getCrac().getPreventiveState())) {
                    networkPool.submit(() -> {
                        try {
                            LOGGER.info("Optimizing curative state {}.", optimizedState.getId());
                            Network networkClone = networkPool.getAvailableNetwork();
                            Set<FlowCnec> cnecs = computePerimeterCnecs(raoInput.getCrac(), stateTree.getPerimeter(optimizedState));
                            LinearOptimizerParameters linearOptimizerParameters = createCurativeLinearOptimizerParameters(raoParameters, stateTree, cnecs);

                            SearchTreeInput searchTreeInput = buildSearchTreeInput(
                                    raoInput.getCrac(),
                                    raoInput.getNetwork(),
                                    optimizedState,
                                    stateTree.getPerimeter(optimizedState),
                                    initialSensitivityOutput,
                                    prePerimeterSensitivityOutput,
                                    curativeTreeParameters,
                                    raoParameters,
                                    linearOptimizerParameters,
                                    toolProvider
                            );

                            OptimizationResult curativeResult = new SearchTree().run(searchTreeInput, curativeTreeParameters, linearOptimizerParameters).join();
                            curativeResults.put(optimizedState, curativeResult);
                            networkPool.releaseUsedNetwork(networkClone);
                            LOGGER.info("Curative state {} has been optimized.", optimizedState.getId());
                        } catch (InterruptedException | NotImplementedException | FaraoException | NullPointerException e) {
                            LOGGER.error("Curative state {} could not be optimized.", optimizedState.getId());
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            });
            networkPool.shutdown();
            networkPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return curativeResults;
    }

    RaoResult mergeRaoResults(PrePerimeterResult initialResult, PerimeterResult preventiveRaoResult, PrePerimeterResult preCurativeResult, Map<State, OptimizationResult> curativeRaoResults) {
        return new PreventiveAndCurativesRaoOutput(initialResult, preventiveRaoResult, preCurativeResult, curativeRaoResults);
    }

    static SearchTreeInput buildSearchTreeInput(Crac crac,
                                                Network network,
                                                State optimizedState,
                                                Set<State> perimeter,
                                                PrePerimeterResult initialOutput,
                                                PrePerimeterResult prePerimeterOutput,
                                                TreeParameters treeParameters,
                                                RaoParameters raoParameters,
                                                LinearOptimizerParameters linearOptimizerParameters,
                                                ToolProvider toolProvider) {
        SearchTreeInput searchTreeInput = new SearchTreeInput();

        searchTreeInput.setNetwork(network);
        Set<FlowCnec> cnecs = computePerimeterCnecs(crac, perimeter);
        searchTreeInput.setFlowCnecs(cnecs);
        searchTreeInput.setNetworkActions(crac.getNetworkActions(optimizedState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED));

        Set<RangeAction> rangeActions = crac.getRangeActions(optimizedState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED);
        removeRangeActionsWithWrongInitialSetpoint(rangeActions, prePerimeterOutput);
        removeAlignedRangeActionsWithDifferentInitialSetpoints(rangeActions, prePerimeterOutput);
        searchTreeInput.setRangeActions(rangeActions);

        ObjectiveFunction objectiveFunction = createObjectiveFunction(
                cnecs,
                initialOutput,
                prePerimeterOutput,
                raoParameters,
                linearOptimizerParameters,
                toolProvider
        );
        searchTreeInput.setObjectiveFunction(objectiveFunction);
        searchTreeInput.setIteratingLinearOptimizer(new IteratingLinearOptimizer(objectiveFunction, raoParameters.getMaxIterations()));

        searchTreeInput.setSearchTreeProblem(new SearchTreeProblem(
                initialOutput,
                prePerimeterOutput,
                prePerimeterOutput,
                cnecs,
                toolProvider.getLoopFlowCnecs(cnecs),
                linearOptimizerParameters
        ));

        SearchTreeComputer.SearchTreeComputerBuilder searchTreeComputerBuilder = SearchTreeComputer.create()
                .withToolProvider(toolProvider)
                .withCnecs(cnecs);
        if (linearOptimizerParameters.hasRelativeMargins()) {
            searchTreeComputerBuilder.withPtdfsResults(initialOutput);
        }
        searchTreeInput.setSearchTreeComputer(searchTreeComputerBuilder.build());

        searchTreeInput.setSearchTreeBloomer(new SearchTreeBloomer(
                network,
                prePerimeterOutput,
                treeParameters.getMaxRa(),
                treeParameters.getMaxTso(),
                treeParameters.getMaxTopoPerTso(),
                treeParameters.getMaxRaPerTso(),
                treeParameters.getSkipNetworkActionsFarFromMostLimitingElement(),
                raoParameters.getExtension(SearchTreeRaoParameters.class).getMaxNumberOfBoundariesForSkippingNetworkActions()
        ));
        searchTreeInput.setPrePerimeterOutput(prePerimeterOutput);

        return searchTreeInput;
    }

    static ObjectiveFunction createObjectiveFunction(Set<FlowCnec> cnecs,
                                                     FlowResult initialFlowResult,
                                                     FlowResult prePerimeterFlowResult,
                                                     RaoParameters raoParameters,
                                                     LinearOptimizerParameters linearOptimizerParameters,
                                                     ToolProvider toolProvider) {
        ObjectiveFunction.ObjectiveFunctionBuilder objectiveFunctionBuilder = ObjectiveFunction.create();
        ObjectiveFunctionHelper.addMinMarginObjectiveFunction(cnecs, prePerimeterFlowResult, objectiveFunctionBuilder, linearOptimizerParameters);
        // TODO : replace this test with a dedicated parameter in RaoParameters
        if (raoParameters.getMnecParameters().getMnecViolationCost() != 0) {
            objectiveFunctionBuilder.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                    cnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                    initialFlowResult,
                    raoParameters.getMnecParameters()
            ));
        }
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            objectiveFunctionBuilder.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                    toolProvider.getLoopFlowCnecs(cnecs),
                    initialFlowResult,
                    raoParameters.getLoopFlowParameters()
            ));
        }
        objectiveFunctionBuilder.withVirtualCostEvaluator(new SensitivityFallbackOvercostEvaluator(
                raoParameters.getFallbackOverCost()
        ));
        return objectiveFunctionBuilder.build();
    }

    public static Set<FlowCnec> computePerimeterCnecs(Crac crac, Set<State> perimeter) {
        if (perimeter != null) {
            Set<FlowCnec> cnecs = new HashSet<>();
            perimeter.forEach(state -> cnecs.addAll(crac.getFlowCnecs(state)));
            return cnecs;
        } else {
            return crac.getFlowCnecs();
        }
    }

    /**
     * If range action's initial setpoint does not respect its allowed range, this function filters it out
     */
    static void removeRangeActionsWithWrongInitialSetpoint(Set<RangeAction> rangeActions, RangeActionResult prePerimeterSetPoints) {
        //a temp set is needed to avoid ConcurrentModificationExceptions when trying to remove a range action from a set we are looping on
        Set<RangeAction> rangeActionsToRemove = new HashSet<>();
        for (RangeAction rangeAction : rangeActions) {
            double preperimeterSetPoint = prePerimeterSetPoints.getOptimizedSetPoint(rangeAction);
            double minSetPoint = rangeAction.getMinAdmissibleSetpoint(preperimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(preperimeterSetPoint);
            if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
                LOGGER.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                        rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
                rangeActionsToRemove.add(rangeAction);
            }
        }
        rangeActionsToRemove.forEach(rangeActions::remove);
    }

    /**
     * If aligned range actionsé initial setpoint are different, this function filters them out
     */
    static void removeAlignedRangeActionsWithDifferentInitialSetpoints(Set<RangeAction> rangeActions, RangeActionResult prePerimeterSetPoints) {
        Set<String> groups = rangeActions.stream().map(RangeAction::getGroupId)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        for (String group : groups) {
            Set<RangeAction> groupRangeActions = rangeActions.stream().filter(rangeAction -> rangeAction.getGroupId().isPresent() && rangeAction.getGroupId().get().equals(group)).collect(Collectors.toSet());
            double preperimeterSetPoint = prePerimeterSetPoints.getOptimizedSetPoint(groupRangeActions.iterator().next());
            if (groupRangeActions.stream().anyMatch(rangeAction -> Math.abs(prePerimeterSetPoints.getOptimizedSetPoint(rangeAction) - preperimeterSetPoint) > 1e-6)) {
                LOGGER.warn("Range actions of group {} do not have the same initial setpoint. They will be filtered out of the linear problem.", group);
                rangeActions.removeAll(groupRangeActions);
            }
        }
    }
}
