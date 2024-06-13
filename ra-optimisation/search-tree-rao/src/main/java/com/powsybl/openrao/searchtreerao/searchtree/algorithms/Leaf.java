/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
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
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
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
public class Leaf {
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
    private final AppliedRemedialActions appliedRemedialActionsInSecondaryStates; // for 2nd prev
    private Network network;
    private final PerimeterResultWithCnecs previousPerimeterResult;
    private final PerimeterResultWithCnecs previousLeafResult;
    private final Set<NetworkAction> networkActionsApplied = new HashSet<>();

    /**
     * Status of the leaf's Network Action evaluation
     */
    private Status status;
    private SearchTreeResult searchTreeResult;
    private ObjectiveFunctionResult objectiveFunctionResult;
    private SearchTreeResult preOptimResult;

    /**
     * Flag indicating whether the data needed for optimization is present
     * It is assumed that data is present initially and that it can be deleted afterwards
     */
    private boolean optimizationDataPresent = true;

    Leaf(OptimizationPerimeter optimizationPerimeter,
         Network network,
         PerimeterResultWithCnecs previousPerimeterResult,
         PerimeterResultWithCnecs previousLeafResult,
         NetworkActionCombination newCombinationToApply,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this.optimizationPerimeter = optimizationPerimeter;
        this.network = network;
        this.previousPerimeterResult = previousPerimeterResult;
        this.appliedRemedialActionsInSecondaryStates = appliedRemedialActionsInSecondaryStates;
        this.previousLeafResult = previousLeafResult;

        // apply Network Actions on initial network
        for (NetworkAction na : newCombinationToApply.getNetworkActionSet()) {
            networkActionsApplied.add(na);
            boolean applicationSuccess = na.apply(network);
            if (!applicationSuccess) {
                throw new OpenRaoException(String.format("%s could not be applied on the network", na.getId()));
            }
        }
        this.status = Status.CREATED;
    }

    Leaf(OptimizationPerimeter optimizationPerimeter,
         Network network,
         PerimeterResultWithCnecs previousPerimeterResult,
         AppliedRemedialActions appliedRemedialActionsInSecondaryStates) {
        this(optimizationPerimeter, network, previousPerimeterResult, null, new NetworkActionCombination(Collections.emptySet()), appliedRemedialActionsInSecondaryStates);
        this.status = Status.EVALUATED;
        this.searchTreeResult = new SearchTreeResult(PerimeterResultWithCnecs.buildFromPreviousResult(previousPerimeterResult), appliedRemedialActionsInSecondaryStates);
        this.preOptimResult = searchTreeResult;
    }

    public FlowResult getPreOptimBranchResult() {
        return preOptimResult.getPerimeterResultWithCnecs();
    }

    public ObjectiveFunctionResult getPreOptimObjectiveFunctionResult() {
        return preOptimResult.getPerimeterResultWithCnecs();
    }

    public Status getStatus() {
        return status;
    }

    boolean isRoot() {
        return preOptimResult.getPerimeterResultWithCnecs().getActivatedNetworkActions().isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     */
    void evaluate(ObjectiveFunction objectiveFunction, SensitivityComputer sensitivityComputer) {
        if (status.equals(Status.EVALUATED)) {
            TECHNICAL_LOGS.debug("Leaf has already been evaluated");
            PerimeterResultWithCnecs preOptimSensiResult = preOptimResult.getPerimeterResultWithCnecs();
            objectiveFunctionResult = objectiveFunction.evaluate(preOptimSensiResult, preOptimSensiResult);
            return;
        }
        TECHNICAL_LOGS.debug("Evaluating {}", this);
        sensitivityComputer.compute(network);
        if (sensitivityComputer.getSensitivityResult().getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_WARNS.warn("Failed to evaluate leaf: sensitivity analysis failed");
            status = Status.ERROR;
            return;
        }
        SensitivityResult preOptimSensitivityResult = sensitivityComputer.getSensitivityResult();
        FlowResult preOptimFlowResult = sensitivityComputer.getBranchResult(network);
        objectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult, preOptimSensitivityResult);
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
                    .withOptimizationPerimeter(optimizationPerimeter)
                    .withInitialFlowResult(searchTreeInput.getInitialFlowResult())
                    .withPrePerimeterResult(previousPerimeterResult)
                    .withPreOptimizationResult(preOptimResult)
                    .withObjectiveFunction(searchTreeInput.getObjectiveFunction())
                    .withToolProvider(searchTreeInput.getToolProvider())
                    .withOutageInstant(searchTreeInput.getOutageInstant())
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
                    .withRaRangeShrinking(parameters.getTreeParameters().raRangeShrinking())
                    .build();

            searchTreeResult = IteratingLinearOptimizer.optimize(linearOptimizerInput, linearOptimizerParameters, searchTreeInput.getOutageInstant());

            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation failed", this);
        } else if (status.equals(Status.CREATED)) {
            BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation has not been performed", this);
        }
    }

    private void resetPreOptimRangeActionsSetpoints(OptimizationPerimeter optimizationContext) {
        optimizationContext.getRangeActionsPerState().forEach((state, rangeActions) ->
                rangeActions.forEach(ra -> ra.apply(network, preOptimResult.getPerimeterResultWithCnecs().getOptimizedSetpoint(ra))));
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

                    limitationParameters.setMaxRangeAction(state, maxRa);
                    limitationParameters.setMaxTso(state, maxTso);
                    limitationParameters.setMaxTsoExclusion(state, tsoWithAlreadyActivatedRa);
                    limitationParameters.setMaxPstPerTso(state, maxPstPerTso);
                    limitationParameters.setMaxRangeActionPerTso(state, maxRaPerTso);
                });
        } else {
            int maxRa = raUsageLimits.getMaxRa() - networkActionsApplied.size();
            Set<String> tsoWithAlreadyActivatedRa = networkActionsApplied.stream().map(RemedialAction::getOperator).collect(Collectors.toSet());
            int maxTso = raUsageLimits.getMaxTso() - tsoWithAlreadyActivatedRa.size();
            Map<String, Integer> maxPstPerTso = raUsageLimits.getMaxPstPerTso();
            Map<String, Integer> maxRaPerTso = new HashMap<>(raUsageLimits.getMaxRaPerTso());
            maxRaPerTso.entrySet().forEach(entry -> {
                int activatedNetworkActionsForTso = networkActionsApplied.stream().filter(na -> entry.getKey().equals(na.getOperator())).collect(Collectors.toSet()).size();
                entry.setValue(entry.getValue() - activatedNetworkActionsForTso);
            });

            limitationParameters.setMaxRangeAction(context.getMainOptimizationState(), maxRa);
            limitationParameters.setMaxTso(context.getMainOptimizationState(), maxTso);
            limitationParameters.setMaxTsoExclusion(context.getMainOptimizationState(), tsoWithAlreadyActivatedRa);
            limitationParameters.setMaxPstPerTso(context.getMainOptimizationState(), maxPstPerTso);
            limitationParameters.setMaxRangeActionPerTso(context.getMainOptimizationState(), maxRaPerTso);
        }
        return limitationParameters;
    }

    public RangeActionResult getRangeActionResult() {
        if (status == Status.EVALUATED) {
            return previousLeafResult.getRangeActionResult();
        } else if (status == Status.OPTIMIZED) {
            return searchTreeResult.getPerimeterResultWithCnecs().getRangeActionResult();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    /**
     * Returns a string with activated network actions
     */
    public String getIdentifier() {
        return isRoot() ? "Root leaf" :
                "network action(s): " + networkActionsApplied.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        String info = getIdentifier();
        if (status.equals(Status.OPTIMIZED)) {
            long nRangeActions = getNumberOfActivatedRangeActions();
            info += String.format(", %s range action(s) activated", nRangeActions > 0 ? nRangeActions : "no");
        }
        PerimeterResultWithCnecs perimeterResultWithCnecs = searchTreeResult.getPerimeterResultWithCnecs();
        if (status.equals(Status.EVALUATED) || status.equals(Status.OPTIMIZED)) {
            Map<String, Double> virtualCostDetailed = getVirtualCostDetailed(perimeterResultWithCnecs);
            info += String.format(Locale.ENGLISH, ", cost: %.2f", perimeterResultWithCnecs.getCost());
            info += String.format(Locale.ENGLISH, " (functional: %.2f", perimeterResultWithCnecs.getFunctionalCost());
            info += String.format(Locale.ENGLISH, ", virtual: %.2f%s)", perimeterResultWithCnecs.getVirtualCost(),
                virtualCostDetailed.isEmpty() ? "" : " " + virtualCostDetailed);
        }
        return info;
    }

    long getNumberOfActivatedRangeActions() {
        if (status == Status.EVALUATED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> preOptimResult.getAllStatesRangeActionResult().getRangeActionResultOnState(s).getActivatedRangeActions().size())
                    .sum();
        } else if (status == Status.OPTIMIZED) {
            return (long) optimizationPerimeter.getRangeActionsPerState().keySet().stream()
                    .mapToDouble(s -> searchTreeResult.getAllStatesRangeActionResult().getRangeActionResultOnState(s).getActivatedRangeActions().size())
                    .sum();
        } else {
            throw new OpenRaoException(NO_RESULTS_AVAILABLE);
        }
    }

    public SearchTreeResult getResult() {
        return searchTreeResult;
    }

    /**
     * Releases data used in optimization to make leaf lighter
     */
    public void finalizeOptimization() {
        this.network = null;
        this.optimizationDataPresent = false;
    }
}
