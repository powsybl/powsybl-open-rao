/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.results.PerimeterStatus;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerInput;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerOutput;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
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

    private final LeafInput leafInput;
    private final RaoParameters raoParameters;
    private final TreeParameters treeParameters;
    private final LinearOptimizerParameters linearOptimizerParameters;

    ObjectiveFunctionEvaluator objectiveFunctionEvaluator;

    private LeafOutput leafOutput;

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

    Leaf(LeafInput leafInput, RaoParameters raoParameters, TreeParameters treeParameters, LinearOptimizerParameters linearOptimizerParameters) {
        this.leafInput = leafInput;
        networkActions = leafInput.getAppliedNetworkActions();
        networkActions.add(leafInput.getNetworkActionToApply());
        this.raoParameters = raoParameters;
        this.treeParameters = treeParameters;
        this.linearOptimizerParameters = linearOptimizerParameters;

        // apply Network Actions on initial network
        networkActions.forEach(na -> na.apply(leafInput.getNetwork()));

        if (leafInput.hasSensitivityAndLoopflowResults()) {
            status = Status.EVALUATED;
        } else {
            status = Status.CREATED;
        }
        objectiveFunctionEvaluator = RaoUtil.createObjectiveFunction(leafInput.getCnecs(), leafInput.getLoopflowCnecs(), leafInput.getPrePerimeterMarginsInAbsoluteMW(),
                leafInput.getInitialCnecResults(), leafInput.getCountriesNotToOptimize(), raoParameters);
    }

    LeafInput getLeafInput() {
        return  leafInput;
    }

    Status getStatus() {
        return status;
    }

    double getOptimizedCost() {
        return leafOutput.getCost();
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
            return;
        }

        try {
            LOGGER.debug("Evaluating leaf...");

            boolean updateSensitivitiesForLoopFlows = raoParameters.isRaoWithLoopFlowLimitation()
                    && raoParameters.getLoopFlowParameters().getLoopFlowApproximationLevel().shouldUpdatePtdfWithTopologicalChange();

            SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, leafInput.getRangeActions(), leafInput.getCnecs(), updateSensitivitiesForLoopFlows, leafInput.getGlskProvider(), leafInput.getLoopflowCnecs());
            SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(leafInput.getNetwork());
            if (updateSensitivitiesForLoopFlows) {
                Map<BranchCnec, Double> commercialFlows = LoopFlowUtil.computeCommercialFlows(leafInput.getNetwork(), leafInput.getLoopflowCnecs(), leafInput.getGlskProvider(), leafInput.getReferenceProgram(), sensitivityResult);
                leafInput.setSensitivityAndLoopflowResults(new SensitivityAndLoopflowResults(sensitivityResult, systematicSensitivityInterface.isFallback(), commercialFlows));
                leafInput.setCommercialFlows(commercialFlows);
            } else {
                leafInput.setSensitivityAndLoopflowResults(new SensitivityAndLoopflowResults(sensitivityResult, systematicSensitivityInterface.isFallback(), leafInput.getCommercialFlows()));
            }

            //TODO: compute this in search tree provider
            status = Status.EVALUATED;
        } catch (FaraoException e) {
            LOGGER.error(String.format("Failed to evaluate leaf: %s", e.getMessage()));
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
            int activatedPstsForTso = (int) leafInput.getRangeActions().stream()
                    .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && isRangeActionActivated(rangeAction))
                    .count();
            int topoLimit =  raLimit - activatedPstsForTso;
            maxTopoPerTso.put(tso, Math.min(topoLimit, maxTopoPerTso.getOrDefault(tso, Integer.MAX_VALUE)));
        });
        return maxTopoPerTso;
    }

    boolean isRangeActionActivated(RangeAction rangeAction) {
        double optimizedSetpoint = leafOutput.getOptimizedSetPoint(rangeAction);
        double preperimeterSetpoint = leafInput.getPrePerimeterSetpoints().get(rangeAction);
        if (Double.isNaN(optimizedSetpoint)) {
            return false;
        } else if (Double.isNaN(preperimeterSetpoint)) {
            return true;
        } else {
            return Math.abs(optimizedSetpoint - preperimeterSetpoint) > 1e-6;
        }
    }

    private IteratingLinearOptimizerInput createIteratingLinearOptimizerInput() {
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, leafInput.getRangeActions(), leafInput.getCnecs(),
                raoParameters.isRaoWithLoopFlowLimitation() && raoParameters.getLoopFlowApproximationLevel().shouldUpdatePtdfWithPstChange(), leafInput.getGlskProvider(), leafInput.getLoopflowCnecs());
        Set<RangeAction> optimizableRangeActions = new HashSet<>(leafInput.getRangeActions());
        Map<RangeAction, Double> optimizableRangeActionSetPoints = new HashMap<>(leafInput.getPrePerimeterSetpoints());
        removeRangeActionsWithWrongInitialSetpoint(optimizableRangeActions, optimizableRangeActionSetPoints, leafInput.getNetwork());
        removeRangeActionsIfMaxNumberReached(optimizableRangeActions, optimizableRangeActionSetPoints, getMaxPstPerTso(),
            objectiveFunctionEvaluator.getMostLimitingElements(leafInput.getSensitivityAndLoopflowResults(), 1).get(0),
            leafInput.getSensitivityAndLoopflowResults().getSystematicSensitivityResult());
        return IteratingLinearOptimizerInput.create()
                .withLoopflowCnecs(leafInput.getLoopflowCnecs())
                .withCnecs(leafInput.getCnecs())
                .withRangeActions(optimizableRangeActions)
                .withNetwork(leafInput.getNetwork())
                .withPreperimeterSetpoints(optimizableRangeActionSetPoints)
                .withPrePerimeterCnecMarginsInMW(leafInput.getPrePerimeterMarginsInAbsoluteMW())
                .withInitialCnecResults(leafInput.getInitialCnecResults())
                .withPreOptimSensitivityResults(leafInput.getSensitivityAndLoopflowResults())
                .withSystematicSensitivityInterface(systematicSensitivityInterface)
                .withObjectiveFunctionEvaluator(objectiveFunctionEvaluator)
                .withGlskProvider(leafInput.getGlskProvider())
                .withReferenceProgram(leafInput.getReferenceProgram())
                .build();
    }

    /**
     * If range action's initial setpoint does not respect its allowed range, this function filters it out
     */
    static void removeRangeActionsWithWrongInitialSetpoint(Set<RangeAction> rangeActions, Map<RangeAction, Double> prePerimeterSetPoints, Network network) {
        //a temp set is needed to avoid ConcurrentModificationExceptions when trying to remove a range action from a set we are looping on
        Set<RangeAction> rangeActionsToRemove = new HashSet<>();
        for (RangeAction rangeAction : rangeActions) {
            double preperimeterSetPoint = prePerimeterSetPoints.get(rangeAction);
            double minSetPoint = rangeAction.getMinValue(network, preperimeterSetPoint);
            double maxSetPoint = rangeAction.getMaxValue(network, preperimeterSetPoint);
            if (preperimeterSetPoint < minSetPoint || preperimeterSetPoint > maxSetPoint) {
                LOGGER.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.",
                    rangeAction.getId(), preperimeterSetPoint, minSetPoint, maxSetPoint);
                rangeActionsToRemove.add(rangeAction);
            }
        }
        rangeActionsToRemove.forEach(rangeAction -> {
            rangeActions.remove(rangeAction);
            prePerimeterSetPoints.remove(rangeAction);
        });
    }

    /**
     * If a TSO has a maximum number of usable ranges actions, this functions filters out the range actions with
     * the least impact on the most limiting element
     */
    static void removeRangeActionsIfMaxNumberReached(Set<RangeAction> rangeActions, Map<RangeAction, Double> prePerimeterSetpoints, Map<String, Integer> maxPstPerTso, BranchCnec mostLimitingElement, SystematicSensitivityResult sensitivityResult) {
        if (!Objects.isNull(maxPstPerTso) && !maxPstPerTso.isEmpty()) {
            maxPstPerTso.forEach((tso, maxPst) -> {
                Set<RangeAction> pstsForTso = rangeActions.stream()
                        .filter(rangeAction -> (rangeAction instanceof PstRangeAction) && rangeAction.getOperator().equals(tso))
                        .collect(Collectors.toSet());
                if (pstsForTso.size() > maxPst) {
                    LOGGER.debug("{} range actions will be filtered out, in order to respect the maximum number of range actions of {} for TSO {}", pstsForTso.size() - maxPst, maxPst, tso);
                    pstsForTso.stream().sorted((ra1, ra2) -> compareAbsoluteSensitivities(ra1, ra2, mostLimitingElement, sensitivityResult))
                            .collect(Collectors.toList()).subList(0, pstsForTso.size() - maxPst)
                            .forEach(rangeAction -> {
                                rangeActions.remove(rangeAction);
                                prePerimeterSetpoints.remove(rangeAction);
                            });
                }
            });
        }
    }

    private static int compareAbsoluteSensitivities(RangeAction ra1, RangeAction ra2, BranchCnec cnec, SystematicSensitivityResult sensitivityResult) {
        Double sensi1 = Math.abs(sensitivityResult.getSensitivityOnFlow(ra1, cnec));
        Double sensi2 = Math.abs(sensitivityResult.getSensitivityOnFlow(ra2, cnec));
        return sensi1.compareTo(sensi2);
    }

    private LeafOutput createLeafOutput(IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput) {
        Set<RangeAction> activatedRangeActions = leafInput.getRangeActions().stream().filter(this::isRangeActionActivated).collect(Collectors.toSet());
        PerimeterStatus perimeterStatus;
        if (iteratingLinearOptimizerOutput.getSensitivityAndLoopflowResults().isFallback()) {
            perimeterStatus = PerimeterStatus.FALLBACK;
        } else {
            perimeterStatus = PerimeterStatus.DEFAULT;
        }
        return new LeafOutput(iteratingLinearOptimizerOutput, iteratingLinearOptimizerOutput, iteratingLinearOptimizerOutput, networkActions, activatedRangeActions, perimeterStatus);
    }

    private LeafOutput createOutputFromPreOptimSituation() {
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = leafInput.getObjectiveFunctionEvaluator();
        SensitivityAndLoopflowResults sensitivityAndLoopflowResults = leafInput.getPreOptimSensitivityResults();
        Network network = iteratingLinearOptimizerInput.getNetwork();

        LinearProblem.SolveStatus solveStatus = LinearProblem.SolveStatus.NOT_SOLVED;
        double functionalCost = objectiveFunctionEvaluator.computeFunctionalCost(sensitivityAndLoopflowResults);
        double virtualCost = objectiveFunctionEvaluator.computeVirtualCost(sensitivityAndLoopflowResults);
        Map<RangeAction, Double> rangeActionSetPoints = new HashMap<>();
        Map<PstRangeAction, Integer> pstTaps = new HashMap<>();
        for (RangeAction rangeAction : iteratingLinearOptimizerInput.getRangeActions()) {
            rangeActionSetPoints.put(rangeAction, rangeAction.getCurrentValue(network));
            if (rangeAction instanceof PstRangeAction) {
                PstRangeAction pstRangeAction = (PstRangeAction) rangeAction;
                pstTaps.put(pstRangeAction, pstRangeAction.getCurrentTapPosition(network, RangeDefinition.CENTERED_ON_ZERO));
            }
        }

        return new IteratingLinearOptimizerOutput(solveStatus, functionalCost, virtualCost, rangeActionSetPoints, pstTaps, sensitivityAndLoopflowResults);
    }

    public LeafOutput getLeafOutput() {
        return leafOutput;
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
            if (!leafInput.getRangeActions().isEmpty()) {
                LOGGER.debug("Optimizing leaf...");
                IteratingLinearOptimizerInput iteratingLinearOptimizerInput = createIteratingLinearOptimizerInput();
                IteratingLinearOptimizerOutput iteratingLinearOptimizerOutput = IteratingLinearOptimizer.optimize(
                        iteratingLinearOptimizerInput, linearOptimizerParameters, raoParameters.getMaxIterations());
                leafOutput = createLeafOutput(iteratingLinearOptimizerOutput);
            } else {
                LOGGER.info("No linear optimization to be performed because no range actions are available");
                leafOutput = createOutputFromPreOptimSituation();
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
        Set<NetworkAction> availableNetworkActions = new HashSet<>(leafInput.getAllNetworkActions()).stream()
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
        CountryGraph countryGraph = new CountryGraph(leafInput.getNetwork());
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters.getSkipNetworkActionsFarFromMostLimitingElement()) {
            Set<Optional<Country>> worstCnecLocation = getOptimizedMostLimitingElementLocation();
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
        Set<Optional<Country>> networkActionCountries = networkAction.getLocation(leafInput.getNetwork());
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

    boolean isFallback() {
        return leafOutput.getStatus().equals(PerimeterStatus.FALLBACK);
    }

    @Override
    public String toString() {
        String info = isRoot() ? "Root leaf" :
                "Network action(s): " + networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
        info += String.format(", Cost: %.2f", getOptimizedCost());
        info += String.format(" (Functional: %.2f", leafOutput.getFunctionalCost());
        info += String.format(", Virtual: %.2f)", leafOutput.getVirtualCost());
        info += ", Status: " + status.getMessage();
        return info;
    }

    private Set<Optional<Country>> getOptimizedMostLimitingElementLocation() {
        BranchCnec cnec = leafOutput.getMostLimitingElements(1).get(0);
        return cnec.getLocation(leafInput.getNetwork());
    }
}
