/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.parameters;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <joris.mancini at rte-france.com>}
 */
public class SearchTreeParameters {

    private final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;

    // required for the search tree algorithm
    private final TreeParameters treeParameters;
    private final NetworkActionParameters networkActionParameters;
    private Map<String, RaUsageLimits> raLimitationParameters;

    // required for sub-module iterating linear optimizer
    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final MnecParametersExtension mnecParameters;
    private final RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
    private final LoopFlowParametersExtension loopFlowParameters;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
    private final int maxNumberOfIterations;

    public SearchTreeParameters(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                TreeParameters treeParameters,
                                NetworkActionParameters networkActionParameters,
                                Map<String, RaUsageLimits> raLimitationParameters,
                                RangeActionsOptimizationParameters rangeActionParameters,
                                MnecParametersExtension mnecParameters,
                                RelativeMarginsParametersExtension maxMinRelativeMarginParameters,
                                LoopFlowParametersExtension loopFlowParameters,
                                UnoptimizedCnecParameters unoptimizedCnecParameters,
                                RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters,
                                int maxNumberOfIterations) {
        this.objectiveFunction = objectiveFunction;
        this.treeParameters = treeParameters;
        this.networkActionParameters = networkActionParameters;
        this.raLimitationParameters = raLimitationParameters;
        this.rangeActionParameters = rangeActionParameters;
        this.mnecParameters = mnecParameters;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
        this.unoptimizedCnecParameters = unoptimizedCnecParameters;
        this.solverParameters = solverParameters;
        this.maxNumberOfIterations = maxNumberOfIterations;
    }

    public ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunction() {
        return objectiveFunction;
    }

    public TreeParameters getTreeParameters() {
        return treeParameters;
    }

    public NetworkActionParameters getNetworkActionParameters() {
        return networkActionParameters;
    }

    public Map<String, RaUsageLimits> getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public MnecParametersExtension getMnecParameters() {
        return mnecParameters;
    }

    public RelativeMarginsParametersExtension getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParametersExtension getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public RangeActionsOptimizationParameters.LinearOptimizationSolver getSolverParameters() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public void setRaLimitationsForSecondPreventive(RaUsageLimits raUsageLimits, Set<RangeAction<?>> rangeActionSet) {
        Set<String> tsoCount = new HashSet<>();
        int raCount = 0;
        Map<String, Integer> currentPstPerTsoLimits = raUsageLimits.getMaxPstPerTso();
        Map<String, Integer> currentRaPerTsoLimits = raUsageLimits.getMaxRaPerTso();
        Map<String, Integer> currentTopoPerTsoLimits = raUsageLimits.getMaxTopoPerTso();
        for (RangeAction<?> rangeAction : rangeActionSet) {
            String tso = rangeAction.getOperator();
            tsoCount.add(tso);
            raCount += 1;
            currentRaPerTsoLimits.computeIfPresent(tso, (key, currentLimit) -> Math.max(0, currentLimit - 1));
            currentPstPerTsoLimits.computeIfPresent(tso, (key, currentLimit) -> Math.max(0, currentLimit - 1));
        }
        raUsageLimits.setMaxRa(Math.max(0, raUsageLimits.getMaxRa() - raCount));
        raUsageLimits.setMaxTso(Math.max(0, raUsageLimits.getMaxTso() - tsoCount.size()));
        currentTopoPerTsoLimits.forEach((tso, raLimits) -> currentTopoPerTsoLimits.put(tso, Math.min(raLimits, currentRaPerTsoLimits.getOrDefault(tso, 1000))));
        currentPstPerTsoLimits.forEach((tso, raLimits) -> currentPstPerTsoLimits.put(tso, Math.min(raLimits, currentRaPerTsoLimits.getOrDefault(tso, 1000))));
        raUsageLimits.setMaxPstPerTso(currentPstPerTsoLimits);
        raUsageLimits.setMaxTopoPerTso(currentTopoPerTsoLimits);
        raUsageLimits.setMaxRaPerTso(currentRaPerTsoLimits);
        this.raLimitationParameters.put("preventive", raUsageLimits);
    }

    public static SearchTreeParametersBuilder create() {
        return new SearchTreeParametersBuilder();
    }

    public static class SearchTreeParametersBuilder {
        private ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
        private TreeParameters treeParameters;
        private NetworkActionParameters networkActionParameters;
        private Map<String, RaUsageLimits> raLimitationParameters;
        private RangeActionsOptimizationParameters rangeActionParameters;
        private MnecParametersExtension mnecParameters;
        private RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
        private LoopFlowParametersExtension loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
        private int maxNumberOfIterations;

        public SearchTreeParametersBuilder withConstantParametersOverAllRao(RaoParameters raoParameters, Crac crac) {
            this.objectiveFunction = raoParameters.getObjectiveFunctionParameters().getType();
            this.networkActionParameters = NetworkActionParameters.buildFromRaoParameters(raoParameters.getTopoOptimizationParameters(), crac);
            this.raLimitationParameters = crac.getRaUsageLimitsPerInstant();
            this.rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);
            this.mnecParameters = raoParameters.getExtension(MnecParametersExtension.class);
            this.maxMinRelativeMarginParameters = raoParameters.getExtension(RelativeMarginsParametersExtension.class);
            this.loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);
            this.solverParameters = raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver();
            this.maxNumberOfIterations = raoParameters.getRangeActionsOptimizationParameters().getMaxMipIterations();
            return this;
        }

        public SearchTreeParametersBuilder with0bjectiveFunction(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
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

        public SearchTreeParametersBuilder withGlobalRemedialActionLimitationParameters(Map<String, RaUsageLimits> raLimitationParameters) {
            this.raLimitationParameters = raLimitationParameters;
            return this;
        }

        public SearchTreeParametersBuilder withRangeActionParameters(RangeActionsOptimizationParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMnecParameters(MnecParametersExtension mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxMinRelativeMarginParameters(RelativeMarginsParametersExtension maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public SearchTreeParametersBuilder withLoopFlowParameters(LoopFlowParametersExtension loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public SearchTreeParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public SearchTreeParametersBuilder withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters) {
            this.solverParameters = solverParameters;
            return this;
        }

        public SearchTreeParametersBuilder withMaxNumberOfIterations(int maxNumberOfIterations) {
            this.maxNumberOfIterations = maxNumberOfIterations;
            return this;
        }

        public SearchTreeParameters build() {
            return new SearchTreeParameters(objectiveFunction,
                treeParameters,
                networkActionParameters,
                raLimitationParameters,
                rangeActionParameters,
                mnecParameters,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                unoptimizedCnecParameters,
                solverParameters,
                maxNumberOfIterations);
        }
    }
}
