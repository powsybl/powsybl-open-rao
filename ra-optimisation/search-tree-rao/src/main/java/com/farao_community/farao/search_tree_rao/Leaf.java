/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.rao_commons.result_api.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A "leaf" is a node of the search tree.
 * Each leaf contains a Network Action, which should be tested in combination with
 * it's parent Leaves' Network Actions
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class Leaf implements OptimizationResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(Leaf.class);
    private static final String NO_RESULTS_AVAILABLE = "No results available.";

    enum Status {
        CREATED("Created"),
        ERROR("Error"),
        EVALUATED("Evaluated"),
        OPTIMIZED("Optimized");
        private String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Network Actions which will be tested (including the
     * network actions from the parent leaves as well as from
     * this leaf), can be empty for root leaf
     */
    private final Set<NetworkAction> networkActions;
    private Network network;
    private final RangeActionResult preOptimRangeActionResult;

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

    Leaf(Network network,
         Set<NetworkAction> alreadyAppliedNetworkActions,
         NetworkActionCombination naCombinationToApply,
         RangeActionResult preOptimRangeActionResult) {
        this.network = network;
        this.preOptimRangeActionResult = preOptimRangeActionResult;
        networkActions = new HashSet<>(alreadyAppliedNetworkActions);
        if (!Objects.isNull(naCombinationToApply)) {
            networkActions.addAll(naCombinationToApply.getNetworkActionSet());
        }

        // apply Network Actions on initial network
        networkActions.forEach(na -> na.apply(network));
        status = Status.CREATED;
    }

    Leaf(Network network, PrePerimeterResult prePerimeterOutput) {
        this(network, Collections.emptySet(), null, prePerimeterOutput);
        status = Status.EVALUATED;
        preOptimFlowResult = prePerimeterOutput;
        preOptimSensitivityResult = prePerimeterOutput;
    }

    public FlowResult getPreOptimBranchResult() {
        return preOptimFlowResult;
    }

    public Status getStatus() {
        return status;
    }

    boolean isRoot() {
        return networkActions.isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     */
    void evaluate(ObjectiveFunction objectiveFunction, SensitivityComputer sensitivityComputer) {
        if (status.equals(Status.EVALUATED)) {
            LOGGER.debug("Leaf has already been evaluated");
            preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult, preOptimSensitivityResult.getSensitivityStatus());
            return;
        }

        try {
            LOGGER.debug("Evaluating leaf...");
            sensitivityComputer.compute(network);
            preOptimSensitivityResult = sensitivityComputer.getSensitivityResult();
            preOptimFlowResult = sensitivityComputer.getBranchResult();
            preOptimObjectiveFunctionResult = objectiveFunction.evaluate(preOptimFlowResult, preOptimSensitivityResult.getSensitivityStatus());
            status = Status.EVALUATED;
        } catch (FaraoException e) {
            LOGGER.error(String.format("Failed to evaluate leaf: %s", e.getMessage()));
            status = Status.ERROR;
        }
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
    void optimize(IteratingLinearOptimizer iteratingLinearOptimizer,
                  SensitivityComputer sensitivityComputer,
                  LeafProblem leafProblem) {
        if (!optimizationDataPresent) {
            throw new FaraoException("Cannot optimize leaf, because optimization data has been deleted");
        }
        if (status.equals(Status.OPTIMIZED)) {
            // If the leaf has already been optimized a first time, reset the setpoints to their pre-optim values
            LOGGER.debug("Resetting range action setpoints to their pre-optim values");
            resetPreOptimRangeActionsSetpoints();
        }
        if (status.equals(Status.EVALUATED) || status.equals(Status.OPTIMIZED)) {
            LOGGER.debug("Optimizing leaf...");
            LinearProblem linearProblem = leafProblem.getLinearProblem(
                    network,
                    preOptimFlowResult,
                    preOptimSensitivityResult
            );
            postOptimResult = iteratingLinearOptimizer.optimize(
                    linearProblem,
                    network,
                    preOptimFlowResult,
                    preOptimSensitivityResult,
                    preOptimRangeActionResult,
                    sensitivityComputer
            );
            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            LOGGER.warn("Impossible to optimize leaf: {}\n because evaluation failed", this);
        } else if (status.equals(Status.CREATED)) {
            LOGGER.warn("Impossible to optimize leaf: {}\n because evaluation has not been performed", this);
        }
    }

    private void resetPreOptimRangeActionsSetpoints() {
        preOptimRangeActionResult.getRangeActions().forEach(rangeAction -> rangeAction.apply(network, preOptimRangeActionResult.getOptimizedSetPoint(rangeAction)));
    }

    @Override
    public String toString() {
        String info = isRoot() ? "Root leaf" :
            "Network action(s): " + networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
        try {
            info += String.format(", Cost: %.2f", getCost());
            info += String.format(" (Functional: %.2f", getFunctionalCost());
            info += String.format(", Virtual: %.2f)", getVirtualCost());
        } catch (FaraoException ignored) {
        }
        info += ", Status: " + status.getMessage();
        return info;
    }

    @Override
    public double getFlow(FlowCnec flowCnec, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getFlow(flowCnec, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getFlow(flowCnec, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getCommercialFlow(FlowCnec flowCnec, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getCommercialFlow(flowCnec, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getCommercialFlow(flowCnec, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getPtdfZonalSum(FlowCnec flowCnec) {
        if (status == Status.EVALUATED) {
            return preOptimFlowResult.getPtdfZonalSum(flowCnec);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getPtdfZonalSum(flowCnec);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<FlowCnec, Double> getPtdfZonalSums() {
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
        return networkActions.contains(networkAction);
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return networkActions;
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
    public Set<RangeAction> getRangeActions() {
        if (status == Status.EVALUATED) {
            return preOptimRangeActionResult.getRangeActions();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getRangeActions();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction) {
        if (status == Status.EVALUATED) {
            return preOptimRangeActionResult.getOptimizedTap(pstRangeAction);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedTap(pstRangeAction);
            } catch (FaraoException e) {
                return preOptimRangeActionResult.getOptimizedTap(pstRangeAction);
            }
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getOptimizedSetPoint(RangeAction rangeAction) {
        if (status == Status.EVALUATED) {
            return preOptimRangeActionResult.getOptimizedSetPoint(rangeAction);
        } else if (status == Status.OPTIMIZED) {
            try {
                return postOptimResult.getOptimizedSetPoint(rangeAction);
            } catch (FaraoException e) {
                return preOptimRangeActionResult.getOptimizedSetPoint(rangeAction);
            }
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTaps() {
        if (status == Status.EVALUATED) {
            return preOptimRangeActionResult.getOptimizedTaps();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedTaps();
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public Map<RangeAction, Double> getOptimizedSetPoints() {
        if (status == Status.EVALUATED) {
            return preOptimRangeActionResult.getOptimizedSetPoints();
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getOptimizedSetPoints();
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
    public double getSensitivityValue(FlowCnec flowCnec, RangeAction rangeAction, Unit unit) {
        if (status == Status.EVALUATED ||
            (status == Status.OPTIMIZED && !postOptimResult.getRangeActions().contains(rangeAction))) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, rangeAction, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, rangeAction, unit);
        } else {
            throw new FaraoException(NO_RESULTS_AVAILABLE);
        }
    }

    @Override
    public double getSensitivityValue(FlowCnec flowCnec, LinearGlsk linearGlsk, Unit unit) {
        if (status == Status.EVALUATED) {
            return preOptimSensitivityResult.getSensitivityValue(flowCnec, linearGlsk, unit);
        } else if (status == Status.OPTIMIZED) {
            return postOptimResult.getSensitivityValue(flowCnec, linearGlsk, unit);
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
