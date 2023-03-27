/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.CurativeOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.PreventiveOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionLimitationParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.IteratingLinearOptimizer;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.search_tree.inputs.SearchTreeInput;
import com.farao_community.farao.search_tree_rao.search_tree.parameters.SearchTreeParameters;
import com.farao_community.farao.sensitivity_analysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

/**
 * A "leaf" is a node of the search tree.
 * Each leaf contains a Network Action, which should be tested in combination with
 * it's parent Leaves' Network Actions
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class Leaf implements OptimizationResult {
    private static final String NO_RESULTS_AVAILABLE = "No results available.";

    public enum Status {
        CREATED,
        ERROR,
        EVALUATED,
        OPTIMIZED
    }

    /**
     * Network Actions which will be tested (including the
     * network actions from the parent leaves as well as from
     * this leaf), can be empty for root leaf
     */
    private final OptimizationPerimeter optimizationPerimeter;
    private final Set<NetworkAction> appliedNetworkActionsInPrimaryState;
    private final AppliedRemedialActions appliedRemedialActionsInSecondaryStates; // for 2nd prev
    private Network network;
    private final RangeActionActivationResult prePerimeterActivationResult;
    private final RangeActionSetpointResult prePerimeterSetpoints;

    /**
     * Status of the leaf's Network Action evaluation
     */
    private Status status;
    private FlowResult preOptimFlowResult;
    private SensitivityResult preOptimSensitivityResult;
    private ObjectiveFunctionResult preOptimObjectiveFunctionResult;
    private LinearOptimizationResult postOptimResult;

    /**
     * Flag indicating whether the data needed for optimization is present
     * It is assumed that data is present initially and that it can be deleted afterwards
     */
    private boolean optimizationDataPresent = true;

    Leaf(OptimizationPerimeter optimizationPerimeter,
         Network network,
         Set<NetworkAction> alreadyAppliedNetworkActionsInPrimaryState,
         NetworkActionCombination newCombinationToApply,
         RangeActionSetpointResult prePerimeterSetpoints,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this.optimizationPerimeter = optimizationPerimeter;
        this.network = network;
        this.prePerimeterActivationResult = new RangeActionActivationResultImpl(prePerimeterSetpoints);
        this.prePerimeterSetpoints = prePerimeterSetpoints;
        if (!Objects.isNull(newCombinationToApply)) {
            this.appliedNetworkActionsInPrimaryState = Stream.concat(
                            alreadyAppliedNetworkActionsInPrimaryState.stream(),
                            newCombinationToApply.getNetworkActionSet().stream())
                    .collect(Collectors.toSet());
        } else {
            this.appliedNetworkActionsInPrimaryState = alreadyAppliedNetworkActionsInPrimaryState;
        }
        this.appliedRemedialActionsInSecondaryStates = appliedRemedialActionsInSecondaryStates;

        // apply Network Actions on initial network
        for (NetworkAction na : appliedNetworkActionsInPrimaryState) {
            boolean applicationSuccess = na.apply(network);
            if (!applicationSuccess) {
                throw new FaraoException(String.format("%s could not be applied on the network", na.getId()));
            }
        }
        this.status = Status.CREATED;
    }

    Leaf(OptimizationPerimeter optimizationPerimeter,
         Network network,
         PrePerimeterResult prePerimeterOutput,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this(optimizationPerimeter, network, Collections.emptySet(), null, prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
        this.status = Status.EVALUATED;
        this.preOptimFlowResult = prePerimeterOutput;
        this.preOptimSensitivityResult = prePerimeterOutput;
    }

    public FlowResult getPreOptimBranchResult() {
        return preOptimFlowResult;
    }

    public Status getStatus() {
        return status;
    }

    boolean isRoot() {
        return appliedNetworkActionsInPrimaryState.isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     */
    void evaluate(ObjectiveFunction objectiveFunction, SensitivityComputer sensitivityComputer) {
        if (status.equals(Status.EVALUATED)) {
            TECHNICAL_LOGS.debug("Leaf has already been evaluated");
            preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult, prePerimeterActivationResult, preOptimSensitivityResult, preOptimSensitivityResult.getSensitivityStatus());
            return;
        }
        TECHNICAL_LOGS.debug("Evaluating {}", this);
        sensitivityComputer.compute(network);
        if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_WARNS.warn("Failed to evaluate leaf: sensitivity analysis failed");
            status = Status.ERROR;
            return;
        }
        preOptimSensitivityResult = sensitivityComputer.getSensitivityResult();
        preOptimFlowResult = sensitivityComputer.getBranchResult(network);
        preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult, prePerimeterActivationResult, preOptimSensitivityResult, preOptimSensitivityResult.getSensitivityStatus());
        status = Status.EVALUATED;
    }

    /**
     * This method tries to optimize range actions on an already evaluated leaf since range action optimization
     * requires computed sensitivity values. Therefore, the leaf is not optimized if leaf status is either ERROR
     * or CREATED (because it means no sensitivity values have already been computed). Once it is performed the status
     * is updated to OPTIMIZED. Besides, the optimization is not performed if no range actions are available
     * in the CRAC to spare computation time but status will still be set to OPTIMIZED meaning no optimization has to
     * be done on this leaf anymore. IteratingLinearOptimizer should never fail so the optimized variant ID in the end
     * is either the same as the initial variant ID if the optimization has not been efficient or a new ID
     * corresponding to a new variant created by the IteratingLinearOptimizer.
     */
    void optimize(SearchTreeInput searchTreeInput, SearchTreeParameters parameters) {
        if (!optimizationDataPresent) {
            throw new FaraoException("Cannot optimize leaf, because optimization data has been deleted");
        }
        if (status.equals(Status.OPTIMIZED)) {
            // If the leaf has already been optimized a first time, reset the setpoints to their pre-optim values
            TECHNICAL_LOGS.debug("Resetting range action setpoints to their pre-optim values");
            resetPreOptimRangeActionsSetpoints(searchTreeInput.getOptimizationPerimeter());
        }
        if (status.equals(Status.EVALUATED) || status.equals(Status.OPTIMIZED)) {
            TECHNICAL_LOGS.debug("Optimizing leaf...");

            // build input
            IteratingLinearOptimizerInput linearOptimizerInput = IteratingLinearOptimizerInput.create()
                    .withNetwork(network)
                    .withOptimizationPerimeter(searchTreeInput.getOptimizationPerimeter())
                    .withInitialFlowResult(searchTreeInput.getInitialFlowResult())
                    .withPrePerimeterFlowResult(searchTreeInput.getPrePerimeterResult())
                    .withPrePerimeterSetpoints(prePerimeterSetpoints)
                    .withPreOptimizationFlowResult(preOptimFlowResult)
                    .withPreOptimizationSensitivityResult(preOptimSensitivityResult)
                    .withPreOptimizationAppliedRemedialActions(appliedRemedialActionsInSecondaryStates)
                    .withRaActivationFromParentLeaf(prePerimeterActivationResult)
                    .withObjectiveFunction(searchTreeInput.getObjectiveFunction())
                    .withToolProvider(searchTreeInput.getToolProvider())
                    .build();

            // build parameters
            IteratingLinearOptimizerParameters linearOptimizerParameters = IteratingLinearOptimizerParameters.create()
                    .withObjectiveFunction(parameters.getObjectiveFunction())
                    .withRangeActionParameters(parameters.getRangeActionParameters())
                    .withMnecParameters(parameters.getMnecParameters())
                    .withMaxMinRelativeMarginParameters(parameters.getMaxMinRelativeMarginParameters())
                    .withLoopFlowParameters(parameters.getLoopFlowParameters())
                    .withUnoptimizedCnecParameters(parameters.getUnoptimizedCnecParameters())
                    .withRaLimitationParameters(getRaLimitationParameters(searchTreeInput.getOptimizationPerimeter(), parameters))
                    .withSolverParameters(parameters.getSolverParameters())
                    .withMaxNumberOfIterations(parameters.getMaxNumberOfIterations())
                    .build();

            postOptimResult = IteratingLinearOptimizer.optimize(linearOptimizerInput, linearOptimizerParameters);

            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation failed", this);
        } else if (status.equals(Status.CREATED)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation has not been performed", this);
        }
    }

    private void resetPreOptimRangeActionsSetpoints(OptimizationPerimeter optimizationContext) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
                rangeActions.forEach(ra -> ra.apply(network, prePerimeterActivationResult.getOptimizedSetpoint(ra, state))));
    }

    private RangeActionLimitationParameters getRaLimitationParameters(OptimizationPerimeter context, SearchTreeParameters parameters) {

        if (context instanceof PreventiveOptimizationPerimeter) {
            // no limitation in preventive
            return null;
        }
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();

        if (context instanceof CurativeOptimizationPerimeter) {

            int maxRa = parameters.getRaLimitationParameters().getMaxCurativeRa() - appliedNetworkActionsInPrimaryState.size();
            Set<String> tsoWithAlreadyActivatedRa = appliedNetworkActionsInPrimaryState.stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
            int maxTso = parameters.getRaLimitationParameters().getMaxCurativeTso() - tsoWithAlreadyActivatedRa.size();
            Map<String, Integer> maxPstPerTso = parameters.getRaLimitationParameters().getMaxCurativePstPerTso();
            Map<String, Integer> maxRaPerTso = new HashMap<>(parameters.getRaLimitationParameters().getMaxCurativeRaPerTso());
            maxRaPerTso.entrySet().forEach(entry -> {
                int activatedNetworkActionsForTso = appliedNetworkActionsInPrimaryState.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
                entry.setValue(entry.getValue() - activatedNetworkActionsForTso);
            });

            limitationParameters.setMaxRangeAction(context.getMainOptimizationState(), maxRa);
            limitationParameters.setMaxTso(context.getMainOptimizationState(), maxTso);
            limitationParameters.setMaxTsoExclusion(context.getMainOptimizationState(), tsoWithAlreadyActivatedRa);
            limitationParameters.setMaxPstPerTso(context.getMainOptimizationState(), maxPstPerTso);
            limitationParameters.setMaxRangeActionPerTso(context.getMainOptimizationState(), maxRaPerTso);

        } else if (context instanceof GlobalOptimizationPerimeter) {

            context.getRangeActionOptimizationStates().stream()
                    .filter(state -> state.getInstant().equals(Instant.CURATIVE))
                    .forEach(state -> {
                        int maxRa = parameters.getRaLimitationParameters().getMaxCurativeRa() - appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).size();
                        Set<String> tsoWithAlreadyActivatedRa = appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
                        int maxTso = parameters.getRaLimitationParameters().getMaxCurativeTso() - tsoWithAlreadyActivatedRa.size();
                        Map<String, Integer> maxPstPerTso = parameters.getRaLimitationParameters().getMaxCurativePstPerTso();
                        Map<String, Integer> maxRaPerTso = new HashMap<>(parameters.getRaLimitationParameters().getMaxCurativeRaPerTso());
                        maxRaPerTso.entrySet().forEach(entry -> {
                            int alreadyActivatedNetworkActionsForTso = appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
                            entry.setValue(entry.getValue() - alreadyActivatedNetworkActionsForTso);
                        });

                        limitationParameters.setMaxRangeAction(state, maxRa);
                        limitationParameters.setMaxTso(state, maxTso);
                        limitationParameters.setMaxTsoExclusion(state, tsoWithAlreadyActivatedRa);
                        limitationParameters.setMaxPstPerTso(state, maxPstPerTso);
                        limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                    });
        }
        return limitationParameters;
    }

    public RangeActionActivationResult getRangeActionActivationResult() {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult;
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getRangeActionActivationResult();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    /**
     * Returns a string with activated network actions
     */
    public String getIdentifier() {
        return isRoot() ? "Root leaf" :
                "network action(s): " + appliedNetworkActionsInPrimaryState.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        String info = getIdentifier();
        if (status.equals(Status.OPTIMIZED)) {
            long nRangeActions = getNumberOfActivatedRangeActions();
            info += String.format(", %s range action(s) activated", nRangeActions > 0 ? nRangeActions : "no");
            info += String.format(Locale.ENGLISH, ", cost: %.2f", getCost());
            info += String.format(Locale.ENGLISH, " (functional: %.2f", getFunctionalCost());
            info += String.format(Locale.ENGLISH, ", virtual: %.2f)", getVirtualCost());
        } else if (status.equals(Status.EVALUATED)) {
            info += ", range actions have not been optimized";
            info += String.format(Locale.ENGLISH, ", cost: %.2f", getCost());
            info += String.format(Locale.ENGLISH, " (functional: %.2f", getFunctionalCost());
            info += String.format(Locale.ENGLISH, ", virtual: %.2f)", getVirtualCost());
        }
        return info;
    }

    long getNumberOfActivatedRangeActions() {
        if (status == Status.EVALUATED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> prePerimeterActivationResult.getActivatedRangeActions(s).size())
                    .sum();
        } else if (status == Status.OPTIMIZED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> postOptimResult.getActivatedRangeActions(s).size())
                    .sum();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, side, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Side side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getCommercialFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, Side side) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getPtdfZonalSum(flowCnec, side);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSum(flowCnec, side);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<FlowCnec, Map<Side, Double>> getPtdfZonalSums() {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getPtdfZonalSums();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSums();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return appliedNetworkActionsInPrimaryState.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return appliedNetworkActionsInPrimaryState;
    }

    @Override
    public double getFunctionalCost() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getFunctionalCost();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFunctionalCost();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getMostLimitingElements(number);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getMostLimitingElements(number);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getVirtualCost() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getVirtualCost();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getVirtualCost();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return preOptimObjectiveFunctionResult.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getVirtualCost(virtualCostName);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getVirtualCost(virtualCostName);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getCostlyElements(virtualCostName, number);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCostlyElements(virtualCostName, number);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getObjectiveFunction();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getObjectiveFunction();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        if (status == Status.EVALUATED) {
            preOptimObjectiveFunctionResult.excludeContingencies(contingenciesToExclude);
        } else if (status == Status.OPTIMIZED) {
            postOptimResult.excludeContingencies(contingenciesToExclude);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }

    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getRangeActions();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getRangeActions();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getActivatedRangeActions(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getActivatedRangeActions(state);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getOptimizedSetpoint(rangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedSetpoint(rangeAction, state);
            } catch (FaraoException e) {
                return prePerimeterActivationResult.getOptimizedSetpoint(rangeAction, state);
            }
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getOptimizedSetpointsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedSetpointsOnState(state);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getOptimizedTap(pstRangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedTap(pstRangeAction, state);
            } catch (FaraoException e) {
                return prePerimeterActivationResult.getOptimizedTap(pstRangeAction, state);
            }
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (status == Status.EVALUATED) {
            return prePerimeterActivationResult.getOptimizedTapsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedTapsOnState(state);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityStatus();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityStatus(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus(state);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<String> getContingencies() {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getContingencies();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getContingencies();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, RangeAction<?> rangeAction, Unit unit) {
        if (status == Status.EVALUATED ||
                (status == Status.OPTIMIZED && !postOptimResult.getRangeActions().contains(rangeAction))) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, Side side, SensitivityVariableSet linearGlsk, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    /**
     * Releases data used in optimization to make leaf lighter
     */
    public void finalizeOptimization() {
        this.network = null;
        this.optimizationDataPresent = false;
    }
}
