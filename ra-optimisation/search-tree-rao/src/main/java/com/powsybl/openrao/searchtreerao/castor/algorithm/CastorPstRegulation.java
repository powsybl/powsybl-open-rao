/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.util.AbstractNetworkPool;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CastorPstRegulation {
    private CastorPstRegulation() {
    }

    public static Set<PstRegulationResult> regulatePsts(List<String> pstsToRegulate, Network network, Crac crac, RaoParameters raoParameters, RaoResult raoResult) {
        // update loadflow parameters
        LoadFlowParameters loadFlowParameters = getLoadFlowParameters(raoParameters);
        boolean initialPhaseShifterRegulationOnValue = loadFlowParameters.isPhaseShifterRegulationOn();
        updateLoadFlowParametersForPstRegulation(loadFlowParameters);

        // apply optimal preventive remedial actions
        applyOptimalRemedialActionsForState(network, raoResult, crac.getPreventiveState());

        // filter out non-curative PSTs
        Set<PstRangeAction> rangeActionsToRegulate = getPstRangeActionsForRegulation(pstsToRegulate, crac);

        // regulate PSTs for each curative scenario in parallel
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), getNumberOfThreads(crac, raoParameters), true)) {
            List<ForkJoinTask<PstRegulationResult>> tasks = crac.getContingencies().stream().map(contingency ->
                networkPool.submit(() -> regulatePstsForContingencyScenario(contingency, crac, pstsToRegulate, rangeActionsToRegulate, raoResult, loadFlowParameters, networkPool, raoParameters.getObjectiveFunctionParameters().getUnit()))
            ).toList();
            Set<PstRegulationResult> pstRegulationResults = new HashSet<>();
            for (ForkJoinTask<PstRegulationResult> task : tasks) {
                try {
                    pstRegulationResults.add(task.get());
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(1000, TimeUnit.SECONDS);
            return pstRegulationResults;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            BUSINESS_WARNS.warn("An error occurred during PST regulation, pre-regulation RAO result will be kept.");
            return Set.of();
        } finally {
            loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
        }
    }

    private static LoadFlowParameters getLoadFlowParameters(RaoParameters raoParameters) {
        return raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters() : new LoadFlowParameters();
    }

    private static void updateLoadFlowParametersForPstRegulation(LoadFlowParameters loadFlowParameters) {
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        if (loadFlowParameters.getExtension(OpenLoadFlowParameters.class) == null) {
            loadFlowParameters.addExtension(OpenLoadFlowParameters.class, new OpenLoadFlowParameters());
        }
        loadFlowParameters.getExtension(OpenLoadFlowParameters.class).setMaxOuterLoopIterations(1000);
    }

    private static void applyOptimalRemedialActionsForState(Network networkClone, RaoResult raoResult, State state) {
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(networkClone));
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> rangeAction.apply(networkClone, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
    }

    private static Set<PstRangeAction> getPstRangeActionsForRegulation(List<String> pstsToRegulate, Crac crac) {
        Map<String, PstRangeAction> rangeActionPerPst = getRangeActionPerPst(pstsToRegulate, crac);
        Set<PstRangeAction> rangeActionsToRegulate = new HashSet<>();
        for (String pstId : pstsToRegulate) {
            if (rangeActionPerPst.containsKey(pstId)) {
                rangeActionsToRegulate.add(rangeActionPerPst.get(pstId));
            } else {
                BUSINESS_LOGS.info("PST {} cannot be regulated as no curative PST range action was defined for it.", pstId);
            }
        }
        return rangeActionsToRegulate;
    }

    private static Map<String, PstRangeAction> getRangeActionPerPst(List<String> pstsToRegulate, Crac crac) {
        // filter out preventive only range actions, as results reporting would be less relevant
        return crac.getPstRangeActions().stream()
            .filter(pstRangeAction -> pstRangeAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant() == crac.getLastInstant()))
            .filter(pstRangeAction -> pstsToRegulate.contains(pstRangeAction.getNetworkElement().getId()))
            .collect(Collectors.toMap(pstRangeAction -> pstRangeAction.getNetworkElement().getId(), Function.identity()));
    }

    private static int getNumberOfThreads(Crac crac, RaoParameters raoParameters) {
        return Math.min(getAvailableCPUs(raoParameters), crac.getContingencies().size());
    }

    private static PstRegulationResult regulatePstsForContingencyScenario(Contingency contingency, Crac crac, List<String> pstsToRegulate, Set<PstRangeAction> rangeActionsToRegulate, RaoResult raoResult, LoadFlowParameters loadFlowParameters, AbstractNetworkPool networkPool, Unit unit) throws InterruptedException {
        Pair<Set<FlowCnec>, Double> mostLimitingElementsAndMargin = getMostLimitingElements(contingency, crac, raoResult, unit);
        boolean regulatePsts = scenarioNeedsRegulation(mostLimitingElementsAndMargin.getKey(), mostLimitingElementsAndMargin.getValue(), pstsToRegulate);
        if (regulatePsts) {
            logPstRegulationTriggeringReason(contingency);
            Network networkClone = networkPool.getAvailableNetwork();
            simulateContingencyAndApplyCurativeActions(contingency, networkClone, crac, raoResult);
            Map<PstRangeAction, Integer> initialTapPerPst = getInitialTapPerPst(rangeActionsToRegulate, networkClone);
            Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(networkClone, rangeActionsToRegulate, loadFlowParameters);
            logPstRegulationResultsForContingencyScenario(contingency, initialTapPerPst, regulatedTapPerPst);
            networkPool.releaseUsedNetwork(networkClone);
            return new PstRegulationResult(contingency, regulatedTapPerPst);
        }
        return new PstRegulationResult(contingency, Map.of());
    }

    private static Pair<Set<FlowCnec>, Double> getMostLimitingElements(Contingency contingency, Crac crac, RaoResult raoResult, Unit unit) {
        Instant lastInstant = crac.getLastInstant();
        Map<FlowCnec, Double> marginPerCnec = crac.getFlowCnecs().stream()
            .filter(flowCnec -> lastInstant.equals(flowCnec.getState().getInstant()))
            .filter(flowCnec -> flowCnec.getState().getContingency().isPresent())
            .filter(flowCnec -> contingency.equals(flowCnec.getState().getContingency().get()))
            .collect(Collectors.toMap(Function.identity(), flowCnec -> raoResult.getMargin(lastInstant, flowCnec, unit)));
        if (marginPerCnec.isEmpty()) {
            return Pair.of(Set.of(), Double.MAX_VALUE);
        }
        // handle the situation when the minimal margin is common to several elements
        List<Map.Entry<FlowCnec, Double>> sortedMargins = marginPerCnec.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
        double minMargin = sortedMargins.get(0).getValue();
        Set<FlowCnec> limitingElements = new HashSet<>();
        sortedMargins.stream().filter(entry -> entry.getValue() == minMargin).forEach(entry -> limitingElements.add(entry.getKey()));
        return Pair.of(limitingElements, minMargin);
    }

    /**
     * This method determines whether the contingency scenario needs PST regulation to be performed after RAO.
     * The conditions for PST regulation to be triggered are that the most limiting element must be defined on one of
     * the PSTs to regulate, and it must be overloaded.
     * @param mostLimitingElements the FlowCnecs with the lowest margin
     * @param margin the margin of the most limiting element
     * @param pstsToRegulate all the PSTs that need to be regulated
     * @return boolean indicating if the regulation conditions are met
     */
    private static boolean scenarioNeedsRegulation(Set<FlowCnec> mostLimitingElements, double margin, List<String> pstsToRegulate) {
        return margin < 0 && mostLimitingElements.stream().anyMatch(limitingElement -> pstsToRegulate.contains(limitingElement.getNetworkElement().getId()));
    }

    private static void simulateContingencyAndApplyCurativeActions(Contingency contingency, Network networkClone, Crac crac, RaoResult raoResult) {
        // simulate contingency
        contingency.toModification().apply(networkClone);

        // apply automatons
        if (crac.hasAutoInstant()) {
            State autoState = crac.getState(contingency, crac.getInstant(InstantKind.AUTO));
            if (autoState != null) {
                applyOptimalRemedialActionsForState(networkClone, raoResult, autoState);
            }
        }

        // apply optimal curative remedial actions
        crac.getInstants(InstantKind.CURATIVE).stream()
            .map(instant -> crac.getState(contingency, instant))
            .filter(Objects::nonNull)
            .forEach(state -> applyOptimalRemedialActionsForState(networkClone, raoResult, state));
    }

    private static Map<PstRangeAction, Integer> getInitialTapPerPst(Set<PstRangeAction> rangeActionsToRegulate, Network networkClone) {
        return rangeActionsToRegulate.stream().collect(Collectors.toMap(Function.identity(), pstRangeAction -> networkClone.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger().getTapPosition()));
    }

    private static void logPstRegulationTriggeringReason(Contingency contingency) {
        BUSINESS_LOGS.info("Contingency scenario '{}': at least one FlowCnec defined on a PST is a limiting element, PST regulation will be performed.", contingency.getId());
    }

    private static void logPstRegulationResultsForContingencyScenario(Contingency contingency,
                                                                      Map<PstRangeAction, Integer> initialTapPerPst,
                                                                      Map<PstRangeAction, Integer> regulatedTapPerPst) {
        List<PstRangeAction> sortedPstRangeActions = initialTapPerPst.keySet().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        List<String> shiftDetails = new ArrayList<>();
        sortedPstRangeActions.forEach(
            pstRangeAction -> {
                int initialTap = initialTapPerPst.get(pstRangeAction);
                int regulatedTap = regulatedTapPerPst.get(pstRangeAction);
                if (initialTap != regulatedTap) {
                    shiftDetails.add("%s (%s -> %s)".formatted(pstRangeAction.getName(), initialTap, regulatedTap));
                }
            }
        );
        if (!shiftDetails.isEmpty()) {
            BUSINESS_LOGS.info("PST regulation for contingency scenario '%s': %s".formatted(contingency.getId(), String.join(", ", shiftDetails)));
        }
    }
}
