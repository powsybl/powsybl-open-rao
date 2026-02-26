/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.parameters;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.*;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.util.*;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getLinearOptimizationSolver;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getMaxMipIterations;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeParameters {

    private final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
    private final Unit flowUnit;

    // required for the search tree algorithm
    private final TreeParameters treeParameters;
    private final NetworkActionParameters networkActionParameters;
    private final Map<Instant, RaUsageLimits> raLimitationParameters;

    // required for sub-module iterating linear optimizer
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;

    private final MnecParameters mnecParameters;
    private final SearchTreeRaoMnecParameters mnecParametersExtension;

    private final SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters;
    private final LoopFlowParameters loopFlowParameters;
    private final SearchTreeRaoLoopFlowParameters loopFlowParametersExtension;

    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final LinearOptimizationSolver solverParameters;
    private final SearchTreeRaoCostlyMinMarginParameters maxMinMarginsParameters;
    private final int maxNumberOfIterations;

    // required for loadflowcomputation (only done if we have HVDC range actions that use HVDC lines in AC emulation)
    // So let's keep it optional
    private final Optional<LoadFlowAndSensitivityParameters> loadFlowAndSensitivityParameters;

    public SearchTreeParameters(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                Unit flowUnit, TreeParameters treeParameters,
                                NetworkActionParameters networkActionParameters,
                                Map<Instant, RaUsageLimits> raLimitationParameters,
                                RangeActionsOptimizationParameters rangeActionParameters,
                                SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                MnecParameters mnecParameters,
                                SearchTreeRaoMnecParameters mnecParametersExtension,
                                SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters,
                                LoopFlowParameters loopFlowParameters,
                                SearchTreeRaoLoopFlowParameters loopFlowParametersExtension,
                                UnoptimizedCnecParameters unoptimizedCnecParameters,
                                LinearOptimizationSolver solverParameters,
                                SearchTreeRaoCostlyMinMarginParameters maxMinMarginParameters,
                                int maxNumberOfIterations,
                                Optional<LoadFlowAndSensitivityParameters> loadFlowAndSensitivityParameters) {
        this.objectiveFunction = objectiveFunction;
        this.flowUnit = flowUnit;
        this.treeParameters = treeParameters;
        this.networkActionParameters = networkActionParameters;
        this.raLimitationParameters = raLimitationParameters;
        this.rangeActionParameters = rangeActionParameters;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.mnecParameters = mnecParameters;
        this.mnecParametersExtension = mnecParametersExtension;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
        this.loopFlowParametersExtension = loopFlowParametersExtension;
        this.unoptimizedCnecParameters = unoptimizedCnecParameters;
        this.solverParameters = solverParameters;
        this.maxMinMarginsParameters = maxMinMarginParameters;
        this.maxNumberOfIterations = maxNumberOfIterations;
        this.loadFlowAndSensitivityParameters = loadFlowAndSensitivityParameters;
    }

    public ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunction() {
        return objectiveFunction;
    }

    public Unit getFlowUnit() {
        return flowUnit;
    }

    public TreeParameters getTreeParameters() {
        return treeParameters;
    }

    public NetworkActionParameters getNetworkActionParameters() {
        return networkActionParameters;
    }

    public Map<Instant, RaUsageLimits> getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public SearchTreeRaoRangeActionsOptimizationParameters getRangeActionParametersExtension() {
        return rangeActionParametersExtension;
    }

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public SearchTreeRaoMnecParameters getMnecParametersExtension() {
        return mnecParametersExtension;
    }

    public SearchTreeRaoRelativeMarginsParameters getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public SearchTreeRaoCostlyMinMarginParameters getMaxMinMarginsParameters() {
        return maxMinMarginsParameters;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public SearchTreeRaoLoopFlowParameters getLoopFlowParametersExtension() {
        return loopFlowParametersExtension;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public LinearOptimizationSolver getSolverParameters() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public Optional<LoadFlowAndSensitivityParameters> getLoadFlowAndSensitivityParameters() {
        return loadFlowAndSensitivityParameters;
    }

    public void decreaseRemedialActionUsageLimits(Map<State, OptimizationResult> resultsPerOptimizationState, Map<State, PrePerimeterResult> prePerimeterResultPerPerimeter) {
        resultsPerOptimizationState.forEach((optimizedState, result) ->
            raLimitationParameters.keySet().forEach(
                otherInstant -> {
                    // Cumulative behaviour of constraints only applies to instants of the same kind
                    if (!otherInstant.comesBefore(optimizedState.getInstant()) && optimizedState.getInstant().getKind().equals(otherInstant.getKind())) {
                        RaUsageLimits raUsageLimits = raLimitationParameters.get(otherInstant);
                        int decreasedMaxRa = decreaseMaxRemedialAction(raUsageLimits, optimizedState, result);
                        Map<String, Integer> decreasedMaxRaPerTso = decreaseMaxRemedialActionPerTso(raUsageLimits, optimizedState, result);
                        Map<String, Integer> decreasedMaxTopoPerTso = decreaseMaxTopoPerTso(raUsageLimits, result, decreasedMaxRaPerTso);
                        Map<String, Integer> decreasedMaxPstPerTso = decreaseMaxPstPerTso(raUsageLimits, optimizedState, result, decreasedMaxRaPerTso);
                        int decreasedMaxTso = decreaseMaxTso(raUsageLimits, optimizedState, result);
                        Map<String, Integer> decreasedMaxElementaryActionsPerTso = decreaseMaxElementaryActionsPerTso(raUsageLimits, optimizedState, result, prePerimeterResultPerPerimeter.get(optimizedState));

                        RaUsageLimits decreasedRaUsageLimits = new RaUsageLimits();
                        decreasedRaUsageLimits.setMaxRa(decreasedMaxRa);
                        decreasedRaUsageLimits.setMaxRaPerTso(decreasedMaxRaPerTso);
                        decreasedRaUsageLimits.setMaxTopoPerTso(decreasedMaxTopoPerTso);
                        decreasedRaUsageLimits.setMaxPstPerTso(decreasedMaxPstPerTso);
                        raUsageLimits.getMaxTsoExclusion().forEach(decreasedRaUsageLimits::addTsoToExclude);
                        getTsoWithActivatedRemedialActionsDuringState(optimizedState, result).forEach(decreasedRaUsageLimits::addTsoToExclude);
                        decreasedRaUsageLimits.setMaxTso(decreasedMaxTso);
                        decreasedRaUsageLimits.setMaxElementaryActionsPerTso(decreasedMaxElementaryActionsPerTso);

                        raLimitationParameters.put(otherInstant, decreasedRaUsageLimits);
                    }
                }
            )
        );
    }

    private static int decreaseMaxRemedialAction(RaUsageLimits raUsageLimits, State optimizedState, OptimizationResult result) {
        return raUsageLimits.getMaxRa() - result.getActivatedNetworkActions().size() - result.getActivatedRangeActions(optimizedState).size();
    }

    private static Map<String, Integer> decreaseMaxTopoPerTso(RaUsageLimits raUsageLimits, OptimizationResult result, Map<String, Integer> decreasedMaxRaPerTso) {
        Map<String, Integer> decreasedMaxTopoPerTso = new HashMap<>();
        raUsageLimits.getMaxTopoPerTso().forEach((key, value) -> decreasedMaxTopoPerTso.put(key, Math.min(value - (int) result.getActivatedNetworkActions().stream().filter(networkAction -> key.equals(networkAction.getOperator())).count(), decreasedMaxRaPerTso.get(key))));
        return decreasedMaxTopoPerTso;
    }

    private static Map<String, Integer> decreaseMaxPstPerTso(RaUsageLimits raUsageLimits, State optimizedState, OptimizationResult result, Map<String, Integer> decreasedMaxRaPerTso) {
        Map<String, Integer> decreasedMaxPstPerTso = new HashMap<>();
        raUsageLimits.getMaxPstPerTso().forEach((key, value) -> decreasedMaxPstPerTso.put(key, Math.min(value - (int) result.getActivatedRangeActions(optimizedState).stream().filter(rangeAction -> key.equals(rangeAction.getOperator())).count(), decreasedMaxRaPerTso.get(key))));
        return decreasedMaxPstPerTso;
    }

    private static Set<String> getTsoWithActivatedRemedialActionsDuringState(State optimizedState, OptimizationResult result) {
        Set<String> tsos = new HashSet<>();
        result.getActivatedNetworkActions().forEach(networkAction -> tsos.add(networkAction.getOperator()));
        result.getActivatedRangeActions(optimizedState).forEach(rangeAction -> tsos.add(rangeAction.getOperator()));
        return tsos;
    }

    private static int decreaseMaxTso(RaUsageLimits raUsageLimits, State optimizedState, OptimizationResult result) {
        Set<String> newTsos = new HashSet<>(getTsoWithActivatedRemedialActionsDuringState(optimizedState, result));
        raUsageLimits.getMaxTsoExclusion().forEach(newTsos::remove);
        return raUsageLimits.getMaxTso() - newTsos.size();
    }

    private static Map<String, Integer> decreaseMaxRemedialActionPerTso(RaUsageLimits raUsageLimits, State optimizedState, OptimizationResult result) {
        Map<String, Integer> decreasedMaxRaPerTso = new HashMap<>();
        raUsageLimits.getMaxRaPerTso().forEach((key, value) -> decreasedMaxRaPerTso.put(key, value - (int) result.getActivatedNetworkActions().stream().filter(networkAction -> key.equals(networkAction.getOperator())).count() - (int) result.getActivatedRangeActions(optimizedState).stream().filter(networkAction -> key.equals(networkAction.getOperator())).count()));
        return decreasedMaxRaPerTso;
    }

    private static Map<String, Integer> decreaseMaxElementaryActionsPerTso(RaUsageLimits raUsageLimits, State optimizedState, OptimizationResult result, PrePerimeterResult prePerimeterResult) {
        Map<String, Integer> decreasedMaxElementaryActionsPerTso = new HashMap<>();
        raUsageLimits.getMaxElementaryActionsPerTso().forEach((tso, eaLimit) -> decreasedMaxElementaryActionsPerTso.put(tso, Math.max(0, eaLimit - computeActivatedElementaryActionsForTso(tso, optimizedState, result, prePerimeterResult))));
        return decreasedMaxElementaryActionsPerTso;
    }

    private static int computeActivatedElementaryActionsForTso(String tso, State optimizedState, OptimizationResult result, PrePerimeterResult prePerimeterResult) {
        return computeActivatedElementaryNetworkActionsForTso(tso, result) + computeTotalTapsMovedForTso(tso, optimizedState, result, prePerimeterResult);
    }

    private static int computeActivatedElementaryNetworkActionsForTso(String tso, OptimizationResult result) {
        return result.getActivatedNetworkActions().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).mapToInt(networkAction -> networkAction.getElementaryActions().size()).sum();
    }

    private static int computeTotalTapsMovedForTso(String tso, State optimizedState, OptimizationResult result, PrePerimeterResult prePerimeterResult) {
        return result.getActivatedRangeActions(optimizedState).stream().filter(rangeAction -> tso.equals(rangeAction.getOperator())).filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).mapToInt(pstRangeAction -> computeTapsMoved(pstRangeAction, optimizedState, result, prePerimeterResult)).sum();
    }

    private static int computeTapsMoved(PstRangeAction pstRangeAction, State optimizedState, OptimizationResult result, PrePerimeterResult prePerimeterResult) {
        return Math.abs(result.getOptimizedTap(pstRangeAction, optimizedState) - prePerimeterResult.getTap(pstRangeAction));
    }

    public static SearchTreeParametersBuilder create() {
        return new SearchTreeParametersBuilder();
    }

    public static class SearchTreeParametersBuilder {
        private ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
        private Unit flowUnit;
        private TreeParameters treeParameters;
        private NetworkActionParameters networkActionParameters;
        private Map<Instant, RaUsageLimits> raLimitationParameters;
        private RangeActionsOptimizationParameters rangeActionParameters;
        private SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
        private MnecParameters mnecParameters;
        private SearchTreeRaoMnecParameters mnecParametersExtension;

        private SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters;
        private LoopFlowParameters loopFlowParameters;
        private SearchTreeRaoLoopFlowParameters loopFlowParametersExtension;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private LinearOptimizationSolver solverParameters;
        private SearchTreeRaoCostlyMinMarginParameters maxMinMarginsParameters;
        private int maxNumberOfIterations;

        private Optional<LoadFlowAndSensitivityParameters> loadFlowAndSensitivityParameters;

        public SearchTreeParametersBuilder withConstantParametersOverAllRao(RaoParameters raoParameters, Crac crac) {
            this.objectiveFunction = raoParameters.getObjectiveFunctionParameters().getType();
            this.flowUnit = RaoUtil.getFlowUnit(raoParameters);
            this.networkActionParameters = NetworkActionParameters.buildFromRaoParameters(raoParameters, crac);
            this.raLimitationParameters = new HashMap<>(crac.getRaUsageLimitsPerInstant());
            this.rangeActionParameters = raoParameters.getRangeActionsOptimizationParameters();
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.rangeActionParametersExtension = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters();
            }
            this.mnecParameters = raoParameters.getMnecParameters().orElse(null);
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.mnecParametersExtension = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters().orElse(null);
            }
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.maxMinRelativeMarginParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRelativeMarginsParameters().orElse(null);
            }
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.maxMinMarginsParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMinMarginsParameters().orElse(null);
            }
            this.loopFlowParameters = raoParameters.getLoopFlowParameters().orElse(null);
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.loopFlowParametersExtension = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters().orElse(null);
            }
            this.solverParameters = getLinearOptimizationSolver(raoParameters);
            this.maxNumberOfIterations = getMaxMipIterations(raoParameters);
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                this.loadFlowAndSensitivityParameters = Optional.ofNullable(raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters());
            } else {
                this.loadFlowAndSensitivityParameters = Optional.empty();
            }
            return this;
        }

        public SearchTreeParametersBuilder with0bjectiveFunction(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public SearchTreeParametersBuilder withFlowUnit(Unit flowUnit) {
            this.flowUnit = flowUnit;
            return this;
        }

        public SearchTreeParametersBuilder withTreeParameters(TreeParameters treeParameters) {
            this.treeParameters = treeParameters;
            return this;
        }

        public SearchTreeParametersBuilder withNetworkActionParameters(NetworkActionParameters networkActionParameters) {
            this.networkActionParameters = networkActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withGlobalRemedialActionLimitationParameters(Map<Instant, RaUsageLimits> raLimitationParameters) {
            this.raLimitationParameters = new HashMap<>(raLimitationParameters);
            return this;
        }

        public SearchTreeParametersBuilder withRangeActionParameters(RangeActionsOptimizationParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withRangeActionParametersExtension(SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension) {
            this.rangeActionParametersExtension = rangeActionParametersExtension;
            return this;
        }

        public SearchTreeParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxMinRelativeMarginParameters(SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public SearchTreeParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public SearchTreeParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withSolverParameters(LinearOptimizationSolver solverParameters) {
            this.solverParameters = solverParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxNumberOfIterations(int maxNumberOfIterations) {
            this.maxNumberOfIterations = maxNumberOfIterations;
            return this;
        }

        public SearchTreeParametersBuilder withMaxMinMarginsParameters(SearchTreeRaoCostlyMinMarginParameters maxMinMarginsParameters) {
            this.maxMinMarginsParameters = maxMinMarginsParameters;
            return this;
        }

        public SearchTreeParametersBuilder withLoadFlowAndSensitivityParameters(LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters) {
            this.loadFlowAndSensitivityParameters = Optional.ofNullable(loadFlowAndSensitivityParameters);
            return this;
        }

        public SearchTreeParameters build() {
            return new SearchTreeParameters(
                objectiveFunction,
                flowUnit,
                treeParameters,
                networkActionParameters,
                raLimitationParameters,
                rangeActionParameters,
                rangeActionParametersExtension,
                mnecParameters,
                mnecParametersExtension,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                loopFlowParametersExtension,
                unoptimizedCnecParameters,
                solverParameters,
                maxMinMarginsParameters,
                maxNumberOfIterations,
                loadFlowAndSensitivityParameters);
        }
    }
}
