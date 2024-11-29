/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.IteratingLinearOptimizer;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.RaoLogger.getVirtualCostDetailed;

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
    private final RangeActionActivationResult raActivationResultFromParentLeaf;
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
         RangeActionActivationResult raActivationResultFromParentLeaf,
         RangeActionSetpointResult prePerimeterSetpoints,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this.optimizationPerimeter = optimizationPerimeter;
        this.network = network;
        this.raActivationResultFromParentLeaf = raActivationResultFromParentLeaf;
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
                throw new OpenRaoException(String.format("%s could not be applied on the network", na.getId()));
            }
        }
        this.status = Status.CREATED;
    }

    Leaf(OptimizationPerimeter optimizationPerimeter,
         Network network,
         PrePerimeterResult prePerimeterOutput,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this(optimizationPerimeter, network, Collections.emptySet(), null, new RangeActionActivationResultImpl(prePerimeterOutput), prePerimeterOutput, appliedRemedialActionsInSecondaryStates);
        this.status = Status.EVALUATED;
        this.preOptimFlowResult = prePerimeterOutput;
        this.preOptimSensitivityResult = prePerimeterOutput;
    }

    public FlowResult getPreOptimBranchResult() {
        return preOptimFlowResult;
    }

    public ObjectiveFunctionResult getPreOptimObjectiveFunctionResult() {
        return preOptimObjectiveFunctionResult;
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
            preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult);
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
        preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult);
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
            throw new OpenRaoException("Cannot optimize leaf, because optimization data has been deleted");
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
                    .withRaActivationFromParentLeaf(raActivationResultFromParentLeaf)
                    .withObjectiveFunction(searchTreeInput.getObjectiveFunction())
                    .withToolProvider(searchTreeInput.getToolProvider())
                    .withOutageInstant(searchTreeInput.getOutageInstant())
                    .build();

            // build parameters
            IteratingLinearOptimizerParameters linearOptimizerParameters = IteratingLinearOptimizerParameters.create()
                    .withObjectiveFunction(parameters.getObjectiveFunction())
                    .withObjectiveFunctionUnit(parameters.getObjectiveFunctionUnit())
                    .withRangeActionParameters(parameters.getRangeActionParameters())
                    .withRangeActionParametersExtension(parameters.getRangeActionParametersExtension())
                    .withMnecParameters(parameters.getMnecParameters())
                    .withMaxMinRelativeMarginParameters(parameters.getMaxMinRelativeMarginParameters())
                    .withLoopFlowParameters(parameters.getLoopFlowParameters())
                    .withUnoptimizedCnecParameters(parameters.getUnoptimizedCnecParameters())
                    .withRaLimitationParameters(getRaLimitationParameters(searchTreeInput.getOptimizationPerimeter(), parameters))
                    .withSolverParameters(parameters.getSolverParameters())
                    .withMaxNumberOfIterations(parameters.getMaxNumberOfIterations())
                    .withRaRangeShrinking(parameters.getTreeParameters().raRangeShrinking())
                    .build();

            postOptimResult = IteratingLinearOptimizer.optimize(linearOptimizerInput, linearOptimizerParameters);

            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {} because evaluation failed", this);
        } else if (status.equals(Status.CREATED)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {} because evaluation has not been performed", this);
        }
    }

    private void resetPreOptimRangeActionsSetpoints(OptimizationPerimeter optimizationContext) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
                rangeActions.forEach(ra -> ra.apply(network, raActivationResultFromParentLeaf.getOptimizedSetpoint(ra, state))));
    }

    RangeActionLimitationParameters getRaLimitationParameters(OptimizationPerimeter context, SearchTreeParameters parameters) {
        if (!parameters.getRaLimitationParameters().containsKey(context.getMainOptimizationState().getInstant())) {
            return null;
        }
        RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().get(context.getMainOptimizationState().getInstant());
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();

        if (context instanceof GlobalOptimizationPerimeter) {
            context.getRangeActionOptimizationStates().stream()
                .filter(state -> state.getInstant().isCurative())
                .forEach(state -> {
                    int maxRa = raUsageLimits.getMaxRa() - appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).size();
                    Set<String> tsoWithAlreadyActivatedRa = appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
                    int maxTso = raUsageLimits.getMaxTso() - tsoWithAlreadyActivatedRa.size();
                    Map<String, Integer> maxPstPerTso = raUsageLimits.getMaxPstPerTso();
                    Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
                    maxRaPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
                        entry.setValue(entry.getValue() - alreadyActivatedNetworkActionsForTso);
                    });
                    Map<String, Integer> maxElementaryActionsPerTso = new HashMap<>(raUsageLimits.getMaxElementaryActionsPerTso());
                    maxElementaryActionsPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedRemedialActionsInSecondaryStates.getAppliedNetworkActions(state).stream().filter(na -> entry.getKey().equals(na.getOperator())).mapToInt(na -> na.getElementaryActions().size()).sum();
                        entry.setValue(Math.max(0, entry.getValue() - alreadyActivatedNetworkActionsForTso));
                    });

                    limitationParameters.setMaxRangeAction(state, maxRa);
                    limitationParameters.setMaxTso(state, maxTso);
                    limitationParameters.setMaxTsoExclusion(state, tsoWithAlreadyActivatedRa);
                    limitationParameters.setMaxPstPerTso(state, maxPstPerTso);
                    limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                    limitationParameters.setMaxElementaryActionsPerTso(state, maxElementaryActionsPerTso);
                });
        } else {
            int maxRa = raUsageLimits.getMaxRa() - appliedNetworkActionsInPrimaryState.size();
            Set<String> tsoWithAlreadyActivatedRa = appliedNetworkActionsInPrimaryState.stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
            int maxTso = raUsageLimits.getMaxTso() - tsoWithAlreadyActivatedRa.size();
            Map<String, Integer> maxPstPerTso = raUsageLimits.getMaxPstPerTso();
            Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
            maxRaPerTso.entrySet().forEach(entry -> {
                int activatedNetworkActionsForTso = appliedNetworkActionsInPrimaryState.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
                entry.setValue(entry.getValue() - activatedNetworkActionsForTso);
            });
            Map<String, Integer> maxElementaryActionsPerTso = new HashMap<>(raUsageLimits.getMaxElementaryActionsPerTso());
            maxElementaryActionsPerTso.entrySet().forEach(entry -> {
                int alreadyActivatedNetworkActionsForTso = appliedNetworkActionsInPrimaryState.stream().filter(na -> entry.getKey().equals(na.getOperator())).mapToInt(na -> na.getElementaryActions().size()).sum();
                entry.setValue(Math.max(0, entry.getValue() - alreadyActivatedNetworkActionsForTso));
            });

            limitationParameters.setMaxRangeAction(context.getMainOptimizationState(), maxRa);
            limitationParameters.setMaxTso(context.getMainOptimizationState(), maxTso);
            limitationParameters.setMaxTsoExclusion(context.getMainOptimizationState(), tsoWithAlreadyActivatedRa);
            limitationParameters.setMaxPstPerTso(context.getMainOptimizationState(), maxPstPerTso);
            limitationParameters.setMaxRangeActionPerTso(context.getMainOptimizationState(), maxRaPerTso);
            limitationParameters.setMaxElementaryActionsPerTso(context.getMainOptimizationState(), maxElementaryActionsPerTso);
        }
        return limitationParameters;
    }

    public RangeActionActivationResult getRangeActionActivationResult() {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf;
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getRangeActionActivationResult();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
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
        }
        if (status.equals(Status.EVALUATED) || status.equals(Status.OPTIMIZED)) {
            Map<String, Double> virtualCostDetailed = getVirtualCostDetailed(this);
            double margin = -getCost();
            info += String.format(Locale.ENGLISH, ", cost: %s", MeasurementRounding.roundValueBasedOnMargin(getCost(), margin, 2).doubleValue());
            info += String.format(Locale.ENGLISH, " (functional: %s", MeasurementRounding.roundValueBasedOnMargin(getFunctionalCost(), margin, 2).doubleValue());
            info += String.format(Locale.ENGLISH, ", virtual: %s%s)", MeasurementRounding.roundValueBasedOnMargin(getVirtualCost(), margin, 2).doubleValue(),
                virtualCostDetailed.isEmpty() ? "" : " " + virtualCostDetailed);
        }
        return info;
    }

    long getNumberOfActivatedRangeActions() {
        if (status == Status.EVALUATED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> raActivationResultFromParentLeaf.getActivatedRangeActions(s).size())
                    .sum();
        } else if (status == Status.OPTIMIZED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> postOptimResult.getActivatedRangeActions(s).size())
                    .sum();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getMargin(flowCnec, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getMargin(flowCnec, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, side, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getFlow(flowCnec, side, unit, instant);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, side, unit, instant);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getCommercialFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getPtdfZonalSum(flowCnec, side);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSum(flowCnec, side);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getPtdfZonalSums();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSums();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
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
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getMostLimitingElements(number);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getMostLimitingElements(number);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getVirtualCost() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getVirtualCost();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getVirtualCost();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<String> getVirtualCostNames() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getVirtualCostNames();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getVirtualCostNames();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getVirtualCost(virtualCostName);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getVirtualCost(virtualCostName);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getCostlyElements(virtualCostName, number);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCostlyElements(virtualCostName, number);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        if (status == Status.EVALUATED) {
            return preOptimObjectiveFunctionResult.getObjectiveFunction();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getObjectiveFunction();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        if (status == Status.EVALUATED) {
            preOptimObjectiveFunctionResult.excludeContingencies(contingenciesToExclude);
        } else if (status == Status.OPTIMIZED) {
            postOptimResult.excludeContingencies(contingenciesToExclude);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }

    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        return optimizationPerimeter.getRangeActions();
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getActivatedRangeActions(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getActivatedRangeActions(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getOptimizedSetpoint(rangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedSetpoint(rangeAction, state);
            } catch (OpenRaoException e) {
                return raActivationResultFromParentLeaf.getOptimizedSetpoint(rangeAction, state);
            }
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getOptimizedSetpointsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedSetpointsOnState(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getOptimizedTap(pstRangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedTap(pstRangeAction, state);
            } catch (OpenRaoException e) {
                return raActivationResultFromParentLeaf.getOptimizedTap(pstRangeAction, state);
            }
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getOptimizedTapsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedTapsOnState(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityStatus();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityStatus(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<String> getContingencies() {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getContingencies();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getContingencies();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        if (status == Status.EVALUATED ||
                status == Status.OPTIMIZED && !postOptimResult.getRangeActions().contains(rangeAction)) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
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
