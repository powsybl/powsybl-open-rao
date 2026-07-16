/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.timecoupledsearchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.MeasurementRounding;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.timecoupledsearchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.timecoupledsearchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.TimeCoupledIteratingLinearOptimizer;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.TimeCoupledIteratingLinearOptimizerInput;
import com.powsybl.openrao.timecoupledsearchtreerao.marmot.results.GlobalLinearOptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.reports.SearchTreeReports;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.FlowResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.LinearOptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.NetworkActionsResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.api.SensitivityResult;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.inputs.TimeCoupledSearchTreeInput;
import com.powsybl.openrao.timecoupledsearchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.timecoupledsearchtreerao.reports.ReportUtils.getVirtualCostDetailed;

/**
 * A "leaf" is a node of the search tree.
 * Each leaf contains a Network Action, which should be tested in combination with
 * its parent Leaves' Network Actions.
 *
 * In time-coupled : one Leaf is tested on all the timestamps simultaneously
 * <li>All the inputs are TemporalData except the set of applied network actions
 * <li>The objective function and the LinearOptimizationResult are global
 * <li>A failure on the sensitivity computation of one of the timestamps leads to stop the leaf creation
 *
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TimeCoupledLeaf implements OptimizationResult {
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
    private final TemporalData<OptimizationPerimeter> optimizationPerimeters;
    private final Set<NetworkAction> appliedNetworkActionsInPrimaryState;
    private final TemporalData<AppliedRemedialActions> appliedRemedialActionsInSecondaryStates;
    private TemporalData<Network> networks;
    private final TemporalData<RangeActionActivationResult> raActivationResultFromParentLeaf;
    private final TemporalData<RangeActionSetpointResult> prePerimeterSetpoints;

    /**
     * Status of the leaf's Network Action evaluation
     */
    private Status status;
    private TemporalData<FlowResult> preOptimFlowResults;
    private TemporalData<SensitivityResult> preOptimSensitivityResults;
    private ObjectiveFunctionResult preOptimObjectiveFunctionResult;
    private GlobalLinearOptimizationResult postOptimResult;

    /**
     * Flag indicating whether the data needed for optimization is present
     * It is assumed that data is present initially and that it can be deleted afterwards
     */
    private boolean optimizationDataPresent = true;

    TimeCoupledLeaf(TemporalData<OptimizationPerimeter> optimizationPerimeters,
                    TemporalData<Network> networks,
                    Set<NetworkAction> alreadyAppliedNetworkActionsInPrimaryState,
                    NetworkActionCombination newCombinationToApply,
                    TemporalData<RangeActionActivationResult> raActivationResultFromParentLeaf,
                    TemporalData<RangeActionSetpointResult> prePerimeterSetpoints,
                    TemporalData<AppliedRemedialActions> appliedRemedialActionsInSecondaryStates) {
        this.optimizationPerimeters = optimizationPerimeters;
        this.networks = networks;
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

        // apply Network Actions on initial network for every timestamp
        // if a failure occurs on one of the timestamps, the leaf creation is not carried on.
        networks.getDataPerTimestamp().forEach((timestamp, network) -> {
            for (NetworkAction na : appliedNetworkActionsInPrimaryState) {
                boolean applicationSuccess = na.apply(network); // deactivate the ac emulation
                if (!applicationSuccess) {
                    throw new OpenRaoException(String.format("%s could not be applied on the network at timestamp %s", na.getId(), timestamp));
                }
            }
        });

        this.status = Status.CREATED;
    }

    TimeCoupledLeaf(TemporalData<OptimizationPerimeter> optimizationPerimeters,
                    TemporalData<Network> networks,
                    TemporalData<PrePerimeterResult> prePerimeterOutputs,
                    TemporalData<AppliedRemedialActions> appliedRemedialActionsInSecondaryStates) {
        this(optimizationPerimeters, networks, Collections.emptySet(), null, prePerimeterOutputs.map(RangeActionActivationResultImpl::new), prePerimeterOutputs.map(prePerimeterOutput -> prePerimeterOutput), appliedRemedialActionsInSecondaryStates);
        this.status = Status.EVALUATED;
        this.preOptimFlowResults = prePerimeterOutputs.map(prePerimeterOutput -> prePerimeterOutput);
        this.preOptimSensitivityResults = prePerimeterOutputs.map(prePerimeterOutput -> prePerimeterOutput);
    }

    public TemporalData<FlowResult> getPreOptimBranchResults() {
        return preOptimFlowResults;
    }

    public ObjectiveFunctionResult getPreOptimObjectiveFunctionResult() {
        return preOptimObjectiveFunctionResult;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRoot() {
        return appliedNetworkActionsInPrimaryState.isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     *
     * In time coupled :
     * one sensitivity computation per timestamp is done sequentially. If one of them fails -> the whole leaf fails.
     * the global objective function is built here.
     */
    void evaluate(final ObjectiveFunction globalObjectiveFunction,
                  final TemporalData<SensitivityComputer> sensitivityComputers,
                  final ReportNode reportNode) {
        Map<OffsetDateTime, FlowResult> preOptimFlowResultsPerTimestamp = new HashMap<>();
        Map<OffsetDateTime, SensitivityResult> preOptimSensitivityResultsPerTimestamp = new HashMap<>();
        if (status.equals(Status.EVALUATED)) {
            TECHNICAL_LOGS.debug("Leaf has already been evaluated");
            preOptimObjectiveFunctionResult = buildGlobalObjectiveFunctionResult(globalObjectiveFunction, reportNode);
            return;
        }
        TECHNICAL_LOGS.debug("Evaluating {}", this);
        // then the systematic sensitivity computation is done sequentially on every timestamp
        for (OffsetDateTime timestamp : sensitivityComputers.getTimestamps()) {
            SensitivityComputer sensitivityComputer = sensitivityComputers.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            sensitivityComputer.compute(network);
            SensitivityResult preOptimSensitivityResult = sensitivityComputer.getSensitivityResult();
            FlowResult preOptimFlowResult = sensitivityComputer.getBranchResult(network);
            if (preOptimSensitivityResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
                // a single timestamp failure fails the whole leaf
                SearchTreeReports.reportFailedToEvaluateLeafSensiFailed(reportNode);
                status = Status.ERROR;
                return;
            }
            preOptimFlowResultsPerTimestamp.put(timestamp, preOptimFlowResult);
            preOptimSensitivityResultsPerTimestamp.put(timestamp, preOptimSensitivityResult);
        }
        preOptimSensitivityResults = new TemporalDataImpl<>(preOptimSensitivityResultsPerTimestamp);
        preOptimFlowResults = new TemporalDataImpl<>(preOptimFlowResultsPerTimestamp);
        preOptimObjectiveFunctionResult = buildGlobalObjectiveFunctionResult(globalObjectiveFunction, reportNode);
        status = Status.EVALUATED;
    }

    /**
     * the pre-optimization results (flow results and sensitivity results) are grouped into one GlobalLinearOptimizationResult
     * in order to evaluate the global objective function.
     */
    private LinearOptimizationResult buildGlobalObjectiveFunctionResult(ObjectiveFunction objectiveFunction, ReportNode reportNode) {
        TemporalData<NetworkActionsResult> networkActionsResults = new TemporalDataImpl<>();
        optimizationPerimeters.getTimestamps().forEach(timestamp -> {
            State mainOptimizationState = optimizationPerimeters.getData(timestamp).orElseThrow().getMainOptimizationState();
            networkActionsResults.put(timestamp, new NetworkActionsResultImpl(Map.of(mainOptimizationState, appliedNetworkActionsInPrimaryState)));
        });
        return new GlobalLinearOptimizationResult(
                preOptimFlowResults,
                preOptimSensitivityResults,
                raActivationResultFromParentLeaf,
                networkActionsResults,
                objectiveFunction,
                LinearProblemStatus.OPTIMAL,
                reportNode
        );
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
     *
     * In time coupled :
     * all the timestamps' range actions are optimized in one global MIP : per-timestamp linear optimizer inputs are
     * built and grouped in a TimeCoupledIteratingLinearOptimizerInput.
     *
     */
    void optimize(final TimeCoupledSearchTreeInput searchTreeInput,
                  final SearchTreeParameters parameters,
                  final int parallelism,
                  final ReportNode reportNode) {
        if (!optimizationDataPresent) {
            throw new OpenRaoException("Cannot optimize leaf, because optimization data has been deleted");
        }
        if (status.equals(Status.OPTIMIZED)) {
            // If the leaf has already been optimized a first time, reset the setpoints to their pre-optim values
            TECHNICAL_LOGS.debug("Resetting range action setpoints to their pre-optim values");
            resetPreOptimRangeActionsSetpoints(searchTreeInput.getOptimizationPerimeters());
        }
        if (status.equals(Status.EVALUATED) || status.equals(Status.OPTIMIZED)) {
            TECHNICAL_LOGS.debug("Optimizing leaf...");
            // make a deep copy of each perimeter and change availableRangeAction
            TemporalData<OptimizationPerimeter> optimizationPerimetersWithFilteredHvdcRangeActions = new TemporalDataImpl<>();
            networks.getDataPerTimestamp().forEach((timestamp, network) -> {
                OptimizationPerimeter optimizationPerimeterWithFilteredHvdcRangeAction = searchTreeInput.getOptimizationPerimeters().getData(timestamp).orElseThrow().copyWithFilteredAvailableHvdcRangeAction(network);
                optimizationPerimetersWithFilteredHvdcRangeActions.put(timestamp, optimizationPerimeterWithFilteredHvdcRangeAction);
            });
            // check if there are still range actions to optimize, if at least one of the timestamps still has range actions the mip is launched
            boolean noRangeActionsLeftToOptimize = optimizationPerimetersWithFilteredHvdcRangeActions.getDataPerTimestamp().values().stream().allMatch(optimizationPerimeter -> optimizationPerimeter.getRangeActions().isEmpty());
            if (noRangeActionsLeftToOptimize) {
                SearchTreeReports.reportNoRangeActionToOptimizeAfterFilteringHvdcRangeActions(reportNode);
                return;
            }

            TimeCoupledIteratingLinearOptimizerInput timeCoupledIteratingLinearOptimizerInput = new TimeCoupledIteratingLinearOptimizerInput(
                    buildLinearOptimizerInputs(searchTreeInput, optimizationPerimetersWithFilteredHvdcRangeActions),
                    searchTreeInput.getGlobalObjectiveFunction(),
                    new TimeCoupledConstraints()
            );

            IteratingLinearOptimizerParameters linearOptimizerParameters = IteratingLinearOptimizerParameters.create()
                .withObjectiveFunction(parameters.getObjectiveFunction())
                .withFlowUnit(parameters.getFlowUnit())
                .withRangeActionParameters(parameters.getRangeActionParameters())
                .withRangeActionParametersExtension(parameters.getRangeActionParametersExtension())
                .withMnecParameters(parameters.getMnecParameters())
                .withMnecParametersExtension(parameters.getMnecParametersExtension())
                .withMaxMinRelativeMarginParameters(parameters.getMaxMinRelativeMarginParameters())
                .withMinMarginParameters(parameters.getMaxMinMarginsParameters())
                .withLoopFlowParameters(parameters.getLoopFlowParameters())
                .withLoopFlowParametersExtension(parameters.getLoopFlowParametersExtension())
                .withUnoptimizedCnecParameters(parameters.getUnoptimizedCnecParameters())
                .withRaLimitationParameters(getRaLimitationParameters(searchTreeInput.getOptimizationPerimeters(), parameters))
                .withSolverParameters(parameters.getSolverParameters())
                .withMaxNumberOfIterations(parameters.getMaxNumberOfIterations())
                .withRaRangeShrinking(parameters.getTreeParameters().raRangeShrinking())
                .build();

            postOptimResult = TimeCoupledIteratingLinearOptimizer.optimize(timeCoupledIteratingLinearOptimizerInput, linearOptimizerParameters, parallelism, reportNode);

            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            SearchTreeReports.reportImpossibleToOptimizeLeafBecauseEvaluationFailed(reportNode, this);
        } else if (status.equals(Status.CREATED)) {
            SearchTreeReports.reportImpossibleToOptimizeLeafBecauseEvaluationNotPerformed(reportNode, this);
        }
    }

    private TemporalData<IteratingLinearOptimizerInput> buildLinearOptimizerInputs(TimeCoupledSearchTreeInput searchTreeInput,
                                                                                   TemporalData<OptimizationPerimeter> filteredPerimeters) {
        Map<OffsetDateTime, IteratingLinearOptimizerInput> linearOptimizerInputPerTimestamp = new HashMap<>();
        optimizationPerimeters.getTimestamps().forEach(timestamp -> {
            Network network = networks.getData(timestamp).orElseThrow();
            OptimizationPerimeter optimizationPerimeter = searchTreeInput.getOptimizationPerimeters().getData(timestamp).orElseThrow();
            IteratingLinearOptimizerInput linearOptimizerInput = IteratingLinearOptimizerInput.create()
                .withNetwork(network)
                .withOptimizationPerimeter(filteredPerimeters.getData(timestamp).orElseThrow())
                .withInitialFlowResult(searchTreeInput.getInitialFlowResults().getData(timestamp).orElseThrow())
                .withPrePerimeterFlowResult(searchTreeInput.getPrePerimeterResults().getData(timestamp).orElseThrow())
                .withPrePerimeterSetpoints(prePerimeterSetpoints.getData(timestamp).orElseThrow())
                .withPreOptimizationFlowResult(preOptimFlowResults.getData(timestamp).orElseThrow())
                .withPreOptimizationSensitivityResult(preOptimSensitivityResults.getData(timestamp).orElseThrow())
                .withPreOptimizationAppliedRemedialActions(appliedRemedialActionsInSecondaryStates.getData(timestamp).orElseThrow())
                .withRaActivationFromParentLeaf(raActivationResultFromParentLeaf.getData(timestamp).orElseThrow())
                .withAppliedNetworkActionsInPrimaryState(new NetworkActionsResultImpl(Map.of(optimizationPerimeter.getMainOptimizationState(), appliedNetworkActionsInPrimaryState)))
                .withObjectiveFunction(searchTreeInput.getGlobalObjectiveFunction()) // global objective function
                .withToolProvider(searchTreeInput.getToolProviders().getData(timestamp).orElseThrow())
                .withOutageInstant(searchTreeInput.getOutageInstants().getData(timestamp).orElseThrow())
                .build();
            linearOptimizerInputPerTimestamp.put(timestamp, linearOptimizerInput);
        });
        return new TemporalDataImpl<>(linearOptimizerInputPerTimestamp);
    }

    /** Resets every timestamp's range action setpoints on its own network to the parent leaf's values before re-optimizing on already-optimized leaf. */
    private void resetPreOptimRangeActionsSetpoints(TemporalData<OptimizationPerimeter> optimizationContexts) {
        optimizationContexts.getTimestamps().forEach(timestamp -> {
            OptimizationPerimeter optimizationContext = optimizationContexts.getData(timestamp).orElseThrow();
            Network network = networks.getData(timestamp).orElseThrow();
            RangeActionActivationResult raActivationFromParent = raActivationResultFromParentLeaf.getData(timestamp).orElseThrow();
            optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
                    rangeActions.forEach(ra -> ra.apply(network, raActivationFromParent.getOptimizedSetpoint(ra, state))));
        });
    }

    /**
     *  This method computes remedial action limitation parameters. Already applied network actions must be taken into account.
     *  In all steps except second preventive, context is main optimization state and appliedNetworkActionsInPrimaryState contain
     *  the state's applied network actions. But during second preventive, primary state refers to preventive, and secondary states to other optimized states.
     */
    RangeActionLimitationParameters getRaLimitationParameters(TemporalData<OptimizationPerimeter> contexts, SearchTreeParameters parameters) {
        Map<String, RaUsageLimits> raUsageLimitsPerInstantId = parameters.getRaLimitationParameters().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getId(), Map.Entry::getValue));
        RangeActionLimitationParameters limitationParameters = new RangeActionLimitationParameters();

        contexts.getTimestamps().forEach(timestamp -> {
            OptimizationPerimeter context = contexts.getData(timestamp).orElseThrow();
            AppliedRemedialActions appliedRasInSecondaryStates = appliedRemedialActionsInSecondaryStates.getData(timestamp).orElseThrow();

            for (State state : context.getRangeActionOptimizationStates()) {
                RaUsageLimits raUsageLimits = raUsageLimitsPerInstantId.get(state.getInstant().getId());
                if (raUsageLimits != null) {
                    Set<NetworkAction> appliedNetworkActions = state.equals(context.getMainOptimizationState()) ?
                        appliedNetworkActionsInPrimaryState : appliedRasInSecondaryStates.getAppliedNetworkActions(state);
                    int maxRa = raUsageLimits.getMaxRa() - appliedNetworkActions.size();
                    Map<String, Integer> maxPstPerTso = raUsageLimits.getMaxPstPerTso();
                    Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
                    maxRaPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedNetworkActions.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(
                            Collectors.toSet()).size();
                        entry.setValue(entry.getValue() - alreadyActivatedNetworkActionsForTso);
                    });
                    Map<String, Integer> maxElementaryActionsPerTso = new HashMap<>(raUsageLimits.getMaxElementaryActionsPerTso());
                    maxElementaryActionsPerTso.entrySet().forEach(entry -> {
                        int alreadyActivatedNetworkActionsForTso = appliedNetworkActions.stream()
                            .filter(na -> entry.getKey().equals(na.getOperator()))
                            .mapToInt(na -> na.getElementaryActions().size())
                            .sum();
                        entry.setValue(Math.max(0, entry.getValue() - alreadyActivatedNetworkActionsForTso));
                    });
                    limitationParameters.setMaxRangeAction(state, maxRa);
                    limitationParameters.setMaxPstPerTso(state, maxPstPerTso);
                    limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                    limitationParameters.setMaxElementaryActionsPerTso(state, maxElementaryActionsPerTso);
                }
            }
        });
        return limitationParameters;
    }

    public TemporalData<RangeActionActivationResult> getRangeActionActivationResults() {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf;
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getRangeActionActivationResultTemporalData();
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

    /** Sums the activated range actions over every timestamp's range action optimization states.*/
    long getNumberOfActivatedRangeActions() {
        if (status == Status.EVALUATED) {
            return optimizationPerimeters.getTimestamps().stream().mapToLong(timestamp -> {
                OptimizationPerimeter perimeter = optimizationPerimeters.getData(timestamp).orElseThrow();
                RangeActionActivationResult parentActivation = raActivationResultFromParentLeaf.getData(timestamp).orElseThrow();
                return perimeter.getRangeActionsPerState().keySet().stream()
                    .mapToLong(s -> parentActivation.getActivatedRangeActions(s).size())
                    .sum();
            }).sum();
        } else if (status == Status.OPTIMIZED) {
            return optimizationPerimeters.getDataPerTimestamp().values().stream().mapToLong(perimeter ->
                    perimeter.getRangeActionsPerState().keySet().stream()
                        .mapToLong(s -> postOptimResult.getActivatedRangeActions(s).size())
                        .sum()).sum();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    // All the getters below were adapted to the TemporalData inputs :
    @Override
    public double getMargin(FlowCnec flowCnec, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getMargin(flowCnec, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getMargin(flowCnec, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, side, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getFlow(FlowCnec flowCnec, TwoSides side, Unit unit, Instant instant) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getFlow(flowCnec, side, unit, instant);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, side, unit, instant);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, TwoSides side, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getCommercialFlow(flowCnec, side, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCommercialFlow(flowCnec, side, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec, TwoSides side) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getPtdfZonalSum(flowCnec, side);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSum(flowCnec, side);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<FlowCnec, Map<TwoSides, Double>> getPtdfZonalSums() {
        if (status == Status.EVALUATED) {
            Map<FlowCnec, Map<TwoSides, Double>> ptdfZonalSums = new HashMap<>();
            preOptimFlowResults.getDataPerTimestamp().values().forEach(flowResult -> ptdfZonalSums.putAll(flowResult.getPtdfZonalSums()));
            return ptdfZonalSums;
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
    public Map<State, Set<NetworkAction>> getActivatedNetworkActionsPerState() {
        // The same shared set is reported at every timestamp's main optimization state
        return optimizationPerimeters.getDataPerTimestamp().values().stream().collect(Collectors.toMap(OptimizationPerimeter::getMainOptimizationState, perimeter -> appliedNetworkActionsInPrimaryState));
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
        // the global objective ranks the CNECs of all the timestamps together : the first element is the worst CNEC across all timestamps
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
    public void excludeCnecs(Set<String> cnecsToExclude) {
        if (status == Status.EVALUATED) {
            preOptimObjectiveFunctionResult.excludeCnecs(cnecsToExclude);
        } else if (status == Status.OPTIMIZED) {
            postOptimResult.excludeCnecs(cnecsToExclude);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }

    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        Set<RangeAction<?>> rangeActionsUnion = new HashSet<>();
        optimizationPerimeters.getDataPerTimestamp().values().forEach(optimizationPerimeter -> rangeActionsUnion.addAll(optimizationPerimeter.getRangeActions()));
        return rangeActionsUnion;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getActivatedRangeActions(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getActivatedRangeActions(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<State, Set<RangeAction<?>>> getActivatedRangeActionsPerState() {
        if (status == Status.EVALUATED) {
            // union of every timestamp's activation maps
            Map<State, Set<RangeAction<?>>> activatedRangeActionsPerState = new HashMap<>();
            raActivationResultFromParentLeaf.getDataPerTimestamp().values().forEach(activation -> activatedRangeActionsPerState.putAll(activation.getActivatedRangeActionsPerState()));
            return activatedRangeActionsPerState;
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getActivatedRangeActionsPerState();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedSetpoint(rangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedSetpoint(rangeAction, state);
            } catch (OpenRaoException e) {
                return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedSetpoint(rangeAction, state);
            }
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedSetpointsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedSetpointsOnState(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getSetPointVariation(rangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSetPointVariation(rangeAction, state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedTap(pstRangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedTap(pstRangeAction, state);
            } catch (OpenRaoException e) {
                return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedTap(pstRangeAction, state);
            }
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getOptimizedTapsOnState(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedTapsOnState(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        if (status == Status.EVALUATED) {
            return raActivationResultFromParentLeaf.getData(getStateTimestamp(state)).orElseThrow().getTapVariation(pstRangeAction, state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getTapVariation(pstRangeAction, state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus() {
        // worst status across all the timestamps : a single failed timestamp degrades the whole leaf's status
        if (status == Status.EVALUATED) {
            return worstSensitivityStatus(preOptimSensitivityResults);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public ComputationStatus getSensitivityStatus(State state) {
        // per-state status on the state's own timestamp
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResults.getData(getStateTimestamp(state)).orElseThrow().getSensitivityStatus(state);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityStatus(state);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Set<String> getContingencies() {
        if (status == Status.EVALUATED) {
            // union of every timestamp's contingencies
            Set<String> contingencies = new HashSet<>();
            preOptimSensitivityResults.getDataPerTimestamp().values().forEach(sensitivityResult -> contingencies.addAll(sensitivityResult.getContingencies()));
            return contingencies;
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getContingencies();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, RangeAction<?> rangeAction, Unit unit) {
        // a range action that was not part of the mip keeps its pre-optimization sensitivity
        if (status == Status.EVALUATED ||
            status == Status.OPTIMIZED && !postOptimResult.getRangeActions().contains(rangeAction)) {
            return preOptimSensitivityResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, rangeAction, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, TwoSides side, SensitivityVariableSet linearGlsk, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResults.getData(getFlowCnecTimestamp(flowCnec)).orElseThrow().getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, side, linearGlsk, unit);
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    /** Aggregates the sensitivity statuses of all timestamps into the worst one. */
    private static ComputationStatus worstSensitivityStatus(TemporalData<SensitivityResult> sensitivityResults) {
        boolean anyFailure = false;
        boolean anyPartialFailure = false;
        for (SensitivityResult sensi : sensitivityResults.getDataPerTimestamp().values()) {
            ComputationStatus status = sensi.getSensitivityStatus();
            if (status == ComputationStatus.FAILURE) {
                anyFailure = true;
            } else if (status == ComputationStatus.PARTIAL_FAILURE) {
                anyPartialFailure = true;
            }
        }
        if (anyFailure) {
            return ComputationStatus.FAILURE;
        }
        if (anyPartialFailure) {
            return ComputationStatus.PARTIAL_FAILURE;
        }
        return ComputationStatus.DEFAULT;
    }

    /**
     * Releases data used in optimization to make leaf lighter
     */
    public void finalizeOptimization() {
        this.networks = null;
        this.optimizationDataPresent = false;
    }

    // helpers
    /** Resolves the timestamp a flow CNEC belongs to. */
    private static OffsetDateTime getFlowCnecTimestamp(FlowCnec flowCnec) {
        return flowCnec.getState().getTimestamp().orElseThrow(() -> new OpenRaoException("FlowCnec " + flowCnec.getId() + " has no timestamp"));
    }

    /** Resolves the timestamp a state belongs to. */
    private static OffsetDateTime getStateTimestamp(State state) {
        return state.getTimestamp().orElseThrow(() -> new OpenRaoException("State has no timestamp"));
    }
}
