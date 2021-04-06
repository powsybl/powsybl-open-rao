/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.LoopFlowUtil;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerOutput;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
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
class Leaf {
    private static final Logger LOGGER = LoggerFactory.getLogger(Leaf.class);

    private final RaoData raoData;
    private final String preOptimVariantId;
    private String optimizedVariantId;
    private final RaoParameters raoParameters;
    private final TreeParameters treeParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    ObjectiveFunctionEvaluator objectiveFunctionEvaluator;

    /**
     * Network Actions which will be tested (including the
     * network actions from the parent leaves as well as from
     * this leaf), can be empty for root leaf
     */
    private final Set<NetworkAction> networkActions;

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
     * Status of the leaf's Network Action evaluation
     */
    private Status status;

    /**
     * Root Leaf constructors
     * It is built directly from a RaoData on which a systematic sensitivity analysis could have already been run or not.
     */
    Leaf(RaoData raoData, RaoParameters raoParameters, TreeParameters treeParameters) {
        this.networkActions = new HashSet<>(); // Root leaf has no network action
        this.raoParameters = raoParameters;
        this.treeParameters = treeParameters;
        this.raoData = raoData;
        preOptimVariantId = raoData.getPreOptimVariantId();
        systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData,
            raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange());

        if (raoData.hasSensitivityValues()) {
            status = Status.EVALUATED;
        } else {
            status = Status.CREATED;
        }

        objectiveFunctionEvaluator = RaoUtil.createObjectiveFunction(raoParameters, treeParameters.getOperatorsNotToOptimize());
    }

    /**
     * Leaf constructor from a parent leaf
     * @param parentLeaf: the parent leaf
     * @param networkAction: the leaf's specific network action (to be added to the parent leaf's network actions)
     * @param network: the Network object
     * @param raoParameters: the RAO parameters
     * @param treeParameters: the Tree parameters
     */
    Leaf(Leaf parentLeaf, NetworkAction networkAction, Network network, RaoParameters raoParameters, TreeParameters treeParameters) {
        networkActions = new HashSet<>(parentLeaf.networkActions);
        networkActions.add(networkAction);
        this.raoParameters = raoParameters;
        this.treeParameters = treeParameters;

        // apply Network Actions on initial network
        networkActions.forEach(na -> na.apply(network));
        // It creates a new CRAC variant
        raoData = RaoData.create(network, parentLeaf.getRaoData());
        preOptimVariantId = raoData.getPreOptimVariantId();
        activateNetworkActionInCracResult(preOptimVariantId);
        systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData,
            raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange());
        raoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(parentLeaf.getRaoData().getPreOptimVariantId(), preOptimVariantId);
        if (!raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange()) {
            raoData.getCracResultManager().copyCommercialFlowsBetweenVariants(parentLeaf.getRaoData().getPreOptimVariantId(), preOptimVariantId);
        }
        status = Status.CREATED;

        objectiveFunctionEvaluator = RaoUtil.createObjectiveFunction(raoParameters, treeParameters.getOperatorsNotToOptimize());
    }

    RaoData getRaoData() {
        return raoData;
    }

    Status getStatus() {
        return status;
    }

    String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    String getBestVariantId() {
        if (status.equals(Status.OPTIMIZED)) {
            return optimizedVariantId;
        } else {
            return preOptimVariantId;
        }
    }

    double getBestCost() {
        return raoData.getCracResult(getBestVariantId()).getCost();
    }

    Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    boolean isRoot() {
        return networkActions.isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     */
    void evaluate() {
        if (status.equals(Status.EVALUATED)) {
            LOGGER.debug("Leaf has already been evaluated");
            SensitivityAndLoopflowResults sensitivityAndLoopflowResults = raoData.getSensitivityAndLoopflowResults();
            raoData.getCracResultManager().fillCracResultWithCosts(
                objectiveFunctionEvaluator.getFunctionalCost(sensitivityAndLoopflowResults), objectiveFunctionEvaluator.getVirtualCost(sensitivityAndLoopflowResults));
            return;
        }

        try {
            LOGGER.debug("Evaluating leaf...");
            raoData.setSystematicSensitivityResult(systematicSensitivityInterface.run(raoData.getNetwork()));
            raoData.getCracResultManager().fillCnecResultWithFlows();
            if (isRoot()) {
                raoData.getCracResultManager().fillPreperimeterCnecResultWithFlows();
            }

            if (raoParameters.isRaoWithLoopFlowLimitation()) {
                LoopFlowUtil.buildLoopFlowsWithLatestSensi(raoData,
                    !raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange());
            }
            SensitivityAndLoopflowResults sensitivityAndLoopflowResults = raoData.getSensitivityAndLoopflowResults();
            raoData.getCracResultManager().fillCracResultWithCosts(
                objectiveFunctionEvaluator.getFunctionalCost(sensitivityAndLoopflowResults), objectiveFunctionEvaluator.getVirtualCost(sensitivityAndLoopflowResults));
            status = Status.EVALUATED;
        } catch (FaraoException e) {
            LOGGER.error(String.format("Fail to evaluate leaf: %s", e.getMessage()));
            status = Status.ERROR;
        }
    }

    /**
     * This function computes the allowed number of PSTs for each TSO, as the minimum between the given parameter
     * and the maximum number of RA reduced by the number of network actions already used
     */
    Map<String, Integer> getMaxPstPerTso() {
        Map<String, Integer> maxPstPerTso = new HashMap<>(treeParameters.getMaxPstPerTso());
        treeParameters.getMaxRaPerTso().forEach((tso, raLimit) -> {
            int appliedNetworkActionsForTso = (int) this.networkActions.stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();
            int pstLimit =  raLimit - appliedNetworkActionsForTso;
            maxPstPerTso.put(tso, Math.min(pstLimit, maxPstPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });
        return maxPstPerTso;
    }

    /**
     * This function computes the allowed number of network actions for each TSO, as the minimum between the given
     * parameter and the maximum number of RA reduced by the number of PSTs already used
     */
    Map<String, Integer> getMaxTopoPerTso() {
        Map<String, Integer> maxTopoPerTso = new HashMap<>(treeParameters.getMaxTopoPerTso());
        treeParameters.getMaxRaPerTso().forEach((tso, raLimit) -> {
            int activatedPstsForTso = (int) raoData.getAvailableRangeActions().stream()
                    .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && isRangeActionActivated(rangeAction))
                    .count();
            int topoLimit =  raLimit - activatedPstsForTso;
            maxTopoPerTso.put(tso, Math.min(topoLimit, maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });
        return maxTopoPerTso;
    }

    boolean isRangeActionActivated(RangeAction rangeAction) {
        double setpoint = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(raoData.getWorkingVariantId()).getSetPoint(raoData.getOptimizedState().getId());
        String prePerimeterVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        double initialSetpoint = rangeAction.getExtension(RangeActionResultExtension.class).getVariant(prePerimeterVariantId).getSetPoint(raoData.getOptimizedState().getId());
        if (Double.isNaN(setpoint)) {
            return false;
        } else if (Double.isNaN(initialSetpoint)) {
            return true;
        } else {
            return Math.abs(initialSetpoint - setpoint) > 1e-6;
        }
    }

    private IteratingLinearOptimizerInput createIteratingLinearOptimizerInput() {
        SystematicSensitivityInterface linearOptimizerSystematicSensitivityInterface =
                RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData,
                        raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithPstChange());
        return IteratingLinearOptimizerInput.create()
                .withLoopflowCnecs(raoData.getLoopflowCnecs())
                .withCnecs(raoData.getCnecs())
                .withRangeActions(raoData.getAvailableRangeActions())
                .withNetwork(raoData.getNetwork())
                .withPreperimeterSetpoints(raoData.getPrePerimeterSetPoints())
                .withInitialCnecResults(raoData.getInitialCnecResults())
                .withPrePerimeterCnecMarginsInAbsoluteMW(raoData.getPrePerimeterMarginsInAbsoluteMW())
                .withPreOptimSensitivityResults(raoData.getSensitivityAndLoopflowResults())
                .withSystematicSensitivityInterface(linearOptimizerSystematicSensitivityInterface)
                .withObjectiveFunctionEvaluator(objectiveFunctionEvaluator)
                .withGlskProvider(raoData.getGlskProvider())
                .withReferenceProgram(raoData.getReferenceProgram())
                .build();
    }

    private IteratingLinearOptimizerParameters createIteratingLinearOptimizerParameters() {
        return IteratingLinearOptimizerParameters.create()
                .withMaxIterations(raoParameters.getMaxIterations())
                .withObjectiveFunction(raoParameters.getObjectiveFunction())
                .withMaxPstPerTso(treeParameters.getMaxPstPerTso())
                .withPstSensitivityThreshold(raoParameters.getPstSensitivityThreshold())
                .withOperatorsNotToOptimize(treeParameters.getOperatorsNotToOptimize())
                .withMnecAcceptableMarginDiminution(raoParameters.getMnecAcceptableMarginDiminution())
                .withLoopFlowApproximationLevel(raoParameters.getLoopFlowApproximationLevel())
                .withLoopFlowConstraintAdjustmentCoefficient(raoParameters.getLoopFlowConstraintAdjustmentCoefficient())
                .withLoopFlowViolationCost(raoParameters.getLoopFlowViolationCost())
                .withLoopFlowAcceptableAugmentation(raoParameters.getLoopFlowAcceptableAugmentation())
                .withMnecViolationCost(raoParameters.getMnecViolationCost())
                .withMnecConstraintAdjustmentCoefficient(raoParameters.getMnecConstraintAdjustmentCoefficient())
                .withNegativeMarginObjectiveCoefficient(raoParameters.getNegativeMarginObjectiveCoefficient())
                .withPstPenaltyCost(raoParameters.getPstPenaltyCost())
                .withPtdfSumLowerBound(raoParameters.getPtdfSumLowerBound())
                .withRaoWithLoopFlowLimitation(raoParameters.isRaoWithLoopFlowLimitation())
                .build();


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
    void optimize() {
        if (status.equals(Status.EVALUATED)) {
            if (!raoData.getAvailableRangeActions().isEmpty()) {;
                LOGGER.debug("Optimizing leaf...");
                //TODO: create this
                IteratingLinearOptimizerInput iteratingLinearOptimizerInput = createIteratingLinearOptimizerInput();
                IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters = createIteratingLinearOptimizerParameters();
                IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = IteratingLinearOptimizer.optimize(iteratingLinearOptimizerInput, iteratingLinearOptimizerParameters);
                //IteratingLinearOptimizer iteratingLinearOptimizer = RaoUtil.createLinearOptimizer(raoParameters, linearOptimizerSystematicSensitivityInterface, getMaxPstPerTso(), treeParameters.getOperatorsNotToOptimize());
                //optimizedVariantId = iteratingLinearOptimizer.optimize(raoData);
                optimizedVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
                raoData.getCracResultManager().fillResultsFromIteratingLinearOptimizerOutput(iteratingLinearOptimizerOutput, optimizedVariantId);
                raoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(preOptimVariantId, optimizedVariantId);
                activateNetworkActionInCracResult(optimizedVariantId);
            } else {
                LOGGER.info("No linear optimization to be performed because no range actions are available");
                optimizedVariantId = preOptimVariantId;
            }
            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            LOGGER.warn("Impossible to optimize leaf: {}\n because evaluation failed", this);
        } else if (status.equals(Status.CREATED)) {
            LOGGER.warn("Impossible to optimize leaf: {}\n because evaluation has not been performed", this);
        }
    }

    /**
     * This method generates a set a of network actions that would be available after this leaf inside the tree. It
     * means all the available network actions in the CRAC except the ones already used in this leaf.
     *
     * @return A set of available network actions after this leaf.
     */
    Set<NetworkAction> bloom() {
        Set<NetworkAction> availableNetworkActions = raoData.getAvailableNetworkActions().stream()
                .filter(na -> !networkActions.contains(na))
                .collect(Collectors.toSet());
        availableNetworkActions = removeNetworkActionsFarFromMostLimitingElement(availableNetworkActions);
        availableNetworkActions = removeNetworkActionsIfMaxNumberReached(availableNetworkActions);
        return availableNetworkActions;
    }

    /**
     * Removes network actions far from most limiting element, using the user's parameters for activating/deactivating this
     * feature, and setting the number of boundaries allowed between the netwrk action and the limiting element
     *
     * @param networkActionsToFilter: the set of network actions to reduce
     * @return the reduced set of network actions
     */
    private Set<NetworkAction> removeNetworkActionsFarFromMostLimitingElement(Set<NetworkAction> networkActionsToFilter) {
        CountryGraph countryGraph = new CountryGraph(raoData.getNetwork());
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters.getSkipNetworkActionsFarFromMostLimitingElement()) {
            Set<Optional<Country>> worstCnecLocation = getMostLimitingElementLocation();
            Set<NetworkAction> filteredNetworkActions = networkActionsToFilter.stream()
                    .filter(na -> isNetworkActionCloseToLocations(na, worstCnecLocation, countryGraph))
                    .collect(Collectors.toSet());
            if (networkActionsToFilter.size() > filteredNetworkActions.size()) {
                LOGGER.debug("{} network actions have been filtered out because they are far from the most limiting element", networkActionsToFilter.size() - filteredNetworkActions.size());
            }
            return filteredNetworkActions;
        } else {
            return networkActionsToFilter;
        }
    }

    /**
     * Removes network actions for whom the maximum number of network actions has been reached
     *
     * @param networkActionsToFilter: the set of network actions to reduce
     * @return the reduced set of network actions
     */
    Set<NetworkAction> removeNetworkActionsIfMaxNumberReached(Set<NetworkAction> networkActionsToFilter) {
        Set<NetworkAction> filteredNetworkActions = new HashSet<>(networkActionsToFilter);
        getMaxTopoPerTso().forEach((String tso, Integer maxTopo) -> {
            long alreadyAppliedForTso = this.networkActions.stream().filter(networkAction -> networkAction.getOperator().equals(tso)).count();
            if (alreadyAppliedForTso >= maxTopo) {
                filteredNetworkActions.removeIf(networkAction -> networkAction.getOperator().equals(tso));
            }
        });
        if (networkActionsToFilter.size() > filteredNetworkActions.size()) {
            LOGGER.debug("{} network actions have been filtered out because the maximum number of network actions for their TSO has been reached", networkActionsToFilter.size() - filteredNetworkActions.size());
        }
        return filteredNetworkActions;
    }

    /**
     * Says if a network action is close to a given set of countries, respecting the maximum number of boundaries
     */
    boolean isNetworkActionCloseToLocations(NetworkAction networkAction, Set<Optional<Country>> locations, CountryGraph countryGraph) {
        if (locations.stream().anyMatch(Optional::isEmpty)) {
            return true;
        }
        Set<Optional<Country>> networkActionCountries = networkAction.getLocation(raoData.getNetwork());
        if (networkActionCountries.stream().anyMatch(Optional::isEmpty)) {
            return true;
        }
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        for (Optional<Country> location : locations) {
            for (Optional<Country> networkActionCountry : networkActionCountries) {
                if (location.isPresent() && networkActionCountry.isPresent()
                        && countryGraph.areNeighbors(location.get(), networkActionCountry.get(), searchTreeRaoParameters.getMaxNumberOfBoundariesForSkippingNetworkActions())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method deletes completely the initial variant if the optimized variant has better results. So it can be
     * used only if the leaf is OPTIMIZED. This method should not be used on root leaf in the tree as long as it
     * is necessary to keep this variant for algorithm results purpose.
     */
    void clearAllVariantsExceptOptimizedOne() {
        if (status.equals(Status.OPTIMIZED) && !preOptimVariantId.equals(optimizedVariantId)) {
            raoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(preOptimVariantId, optimizedVariantId);
            raoData.getCracVariantManager().deleteVariant(preOptimVariantId, false);
        }
    }

    /**
     * This method deletes all the variants of the leaf rao data, except the initial variant. It is useful as when the
     * tree's optimal leaf is not the root leaf we don't want to see in the CracResult any other variant than the initial
     * one (from the root leaf) and the best variant (from the optimal leaf).
     * Thus this method should be called only on the root leaf.
     */
    void clearAllVariantsExceptInitialOne() {
        HashSet<String> variantIds = new HashSet<>();
        variantIds.addAll(raoData.getCracVariantManager().getVariantIds());
        variantIds.remove(preOptimVariantId);
        raoData.getCracVariantManager().setWorkingVariant(preOptimVariantId);
        variantIds.forEach(variantId -> raoData.getCracVariantManager().deleteVariant(variantId, false));
    }

    /**
     * This method deletes all the variants of the leaf rao data meaning at least the initial variant and most the
     * initial variant and the optimized variant. It is a delegate method to avoid calling directly rao data as a leaf
     * user.
     */
    void clearAllVariants() {
        raoData.getCracVariantManager().clear();
    }

    /**
     * This method activates network actions related to this leaf in the CRAC results. This action has to be done
     * every time a new variant is created inside this leaf to ensure results consistency.
     *
     * @param variantId: The ID of the variant to update.
     */
    private void activateNetworkActionInCracResult(String variantId) {
        String stateId = raoData.getOptimizedState().getId();
        for (NetworkAction networkAction : networkActions) {
            networkAction.getExtension(NetworkActionResultExtension.class).getVariant(variantId).activate(stateId);
        }
    }

    @Override
    public String toString() {
        String info = isRoot() ? "Root leaf" :
                "Network action(s): " + networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
        info += String.format(", Cost: %.2f", getBestCost());
        info += String.format(" (Functional: %.2f", raoData.getCracResult().getFunctionalCost());
        info += String.format(", Virtual: %.2f)", raoData.getCracResult().getVirtualCost());
        info += ", Status: " + status.getMessage();
        return info;
    }

    private Set<Optional<Country>> getMostLimitingElementLocation() {
        BranchCnec cnec = RaoUtil.getMostLimitingElement(raoData.getCnecs(), getBestVariantId(),
                raoParameters.getObjectiveFunction().getUnit(), raoParameters.getObjectiveFunction().relativePositiveMargins());
        return cnec.getLocation(raoData.getNetwork());
    }
}
