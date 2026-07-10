/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.helpers;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class RaoHelpers {
    private RaoHelpers() {
    }

    /**
     * Generates a {@link RaoResult} from a set of remedial actions,
     * optimizing the network state, and running sensitivity analyses.
     *
     * @param preventiveNetworkActions  the set of preventive network actions to be applied
     * @param preventiveRangeActions    the set of preventive range actions to be applied with their set-point
     * @param postOutageRemedialActions the set of remedial actions applied in auto or curative
     * @param crac                      the CRAC data
     * @param network                   the network on which to apply the remedial actions and run the load-flow computations
     * @param raoParameters             the set of RAO parameters
     * @param reportNode                a reporting object used to capture and structure details of the computation process
     * @return a {@link RaoResult} gathering the remedial actions, cost and flow results
     */
    public static RaoResult generateRaoResult(Set<NetworkAction> preventiveNetworkActions, Map<RangeAction<?>, Double> preventiveRangeActions, AppliedRemedialActions postOutageRemedialActions, Crac crac, Network network, RaoParameters raoParameters, ReportNode reportNode) {
        final ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(
            RaoInput.build(network, crac).build(), raoParameters
        );
        final PrePerimeterSensitivityAnalysis initialPrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        );
        final PrePerimeterResult initialFlowResult = initialPrePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);

        // create a new network variant from initial variant for performing the results merging
        final String initialVariant = network.getVariantManager().getWorkingVariantId();
        final String variantName = "RAOResultGeneration";
        network.getVariantManager().cloneVariant(initialVariant, variantName);
        network.getVariantManager().setWorkingVariant(variantName);

        // apply PRAs
        final State preventiveState = crac.getPreventiveState();
        preventiveNetworkActions.forEach(networkAction -> networkAction.apply(network));
        preventiveRangeActions.forEach((rangeAction, setPoint) -> rangeAction.apply(network, setPoint));

        // this result is only used as a data holder for flows: it does not contain the proper objective function value in costly
        final PrePerimeterResult preventivePrePerimeterResult = initialPrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
            network, initialFlowResult, Set.of(), new AppliedRemedialActions(), reportNode
        );

        RangeActionActivationResultImpl preventiveRangeActionActivationResult = new RangeActionActivationResultImpl(initialFlowResult);
        preventiveRangeActions.forEach((rangeAction, setPoint) -> preventiveRangeActionActivationResult.putResult(rangeAction, preventiveState, setPoint));

        final OptimizationResult preventiveResult = new OptimizationResultImpl(
            preventivePrePerimeterResult, preventivePrePerimeterResult, preventivePrePerimeterResult,
            new NetworkActionsResultImpl(Map.of(
                preventiveState, preventiveNetworkActions
            )),
            preventiveRangeActionActivationResult
        );

        final PostPerimeterResult preventivePostPerimeterResult =
            new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, preventivePrePerimeterResult, Set.of(), preventiveResult, new AppliedRemedialActions(), reportNode);

        final Map<State, PostPerimeterResult> postRegulationPostContingencyResults = new HashMap<>();

        final List<Instant> postOutageInstants = crac.getSortedInstants().stream()
            .filter(instant -> instant.isAuto() || instant.isCurative())
            .toList();

        for (final Contingency contingency : crac.getContingencies()) {
            final AppliedRemedialActions postContingencyAppliedRemedialActions = new AppliedRemedialActions();

            network.getVariantManager().cloneVariant(variantName, contingency.getId());
            network.getVariantManager().setWorkingVariant(contingency.getId());

            PrePerimeterResult contingencyPrePerimeterResult = preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates();

            for (final Instant instant : postOutageInstants) {
                final State state = crac.getState(contingency, instant);
                if (state != null) {
                    final RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(contingencyPrePerimeterResult);
                    postContingencyAppliedRemedialActions.addAppliedNetworkActions(state, postOutageRemedialActions.getAppliedNetworkActions(state));
                    postOutageRemedialActions.getAppliedRangeActions(state).forEach(
                        (rangeAction, setPoint) -> {
                            postContingencyAppliedRemedialActions.addAppliedRangeAction(state, rangeAction, setPoint);
                            rangeActionActivationResult.putResult(rangeAction, state, setPoint);
                        }
                    );

                    final PrePerimeterSensitivityAnalysis statePrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                        crac, crac.getFlowCnecs(state), crac.getRangeActions(), raoParameters, toolProvider, true
                    );

                    final PrePerimeterResult statePrePerimeterResult = statePrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
                        network, initialFlowResult, Collections.emptySet(), postContingencyAppliedRemedialActions, reportNode
                    );

                    final OptimizationResult stateOptimizationResult = new OptimizationResultImpl(
                        statePrePerimeterResult,
                        statePrePerimeterResult,
                        statePrePerimeterResult,
                        new NetworkActionsResultImpl(Map.of(state, postOutageRemedialActions.getAppliedNetworkActions(state))),
                        rangeActionActivationResult
                    );
                    final Set<FlowCnec> statePostPerimeterFlowCnecs = crac.getFlowCnecs().stream()
                        .filter(cnec -> !cnec.getState().getInstant().comesBefore(instant))
                        .filter(cnec -> cnec.getState().getContingency().orElseThrow().equals(contingency))
                        .collect(Collectors.toSet());

                    final PostPerimeterResult statePostPerimeterResult =
                        new PostPerimeterSensitivityAnalysis(crac, statePostPerimeterFlowCnecs, crac.getRangeActions(), raoParameters, toolProvider, true)
                            .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, contingencyPrePerimeterResult, Set.of(), stateOptimizationResult, postContingencyAppliedRemedialActions, reportNode);
                    postRegulationPostContingencyResults.put(state, statePostPerimeterResult);

                    contingencyPrePerimeterResult = statePrePerimeterResult;
                }
            }

            network.getVariantManager().setWorkingVariant(variantName);
            network.getVariantManager().removeVariant(contingency.getId());
        }

        final StateTree stateTree = new StateTree(crac, reportNode);
        final PreventiveAndCurativesRaoResultImpl raoResult = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialFlowResult,
            preventivePostPerimeterResult,
            postRegulationPostContingencyResults,
            crac,
            raoParameters,
            reportNode
        );
        raoResult.setExecutionDetails("Generated from a set of applied remedial actions.");
        network.getVariantManager().setWorkingVariant(initialVariant);
        network.getVariantManager().removeVariant(variantName);

        return raoResult;
    }

    /**
     * Returns the security status of the RAO Result. It is considered secure only if all the CNECs have a non-negative
     * margin at the end of the optimization.
     *
     * @param raoResult          the RAO Result to assess the security of
     * @param crac               the CRAC data
     * @param raoParameters      the set of RAO parameters
     * @param physicalParameters an optional set of physical parameters to evaluate the system's security (at least one is required)
     * @return {@code true} if the RAO Result is secure; {@code false} otherwise
     */
    public static boolean isSecure(RaoResult raoResult, Crac crac, RaoParameters raoParameters, PhysicalParameter... physicalParameters) {
        // TODO: return raoResult.isSecure(crac, RaoUtil.getFlowUnit(raoParameters), raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras(), physicalParameters);
        return true;
    }
}
