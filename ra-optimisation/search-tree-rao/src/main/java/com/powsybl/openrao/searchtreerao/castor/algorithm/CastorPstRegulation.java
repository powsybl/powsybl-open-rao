/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
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
        // apply optimal preventive remedial actions
        applyOptimalRemedialActionsForState(network, raoResult, crac.getPreventiveState());

        // filter out non-curative PSTs
        Set<PstRangeAction> rangeActionsToRegulate = getPstRangeActionsForRegulation(pstsToRegulate, crac);

        // regulate PSTs for each curative scenario in parallel
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), getAvailableCPUs(raoParameters), true)) {
            List<ForkJoinTask<PstRegulationResult>> tasks = crac.getContingencies().stream().map(contingency ->
                networkPool.submit(() -> regulatePstsForContingencyScenario(contingency, networkPool.getAvailableNetwork(), crac, rangeActionsToRegulate, raoResult, raoParameters))
            ).toList();
            Set<PstRegulationResult> pstRegulationResults = new HashSet<>();
            for (ForkJoinTask<PstRegulationResult> task : tasks) {
                try {
                    pstRegulationResults.add(task.get());
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            return pstRegulationResults;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            BUSINESS_WARNS.warn("An error occurred during PST regulation, pre-regulation RAO result will be kept.");
            return Set.of();
        }
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
        return crac.getPstRangeActions().stream()
            .filter(pstRangeAction -> pstRangeAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant() == crac.getLastInstant()))
            .filter(pstRangeAction -> pstsToRegulate.contains(pstRangeAction.getNetworkElement().getId()))
            .collect(Collectors.toMap(pstRangeAction -> pstRangeAction.getNetworkElement().getId(), Function.identity()));
    }

    private static PstRegulationResult regulatePstsForContingencyScenario(Contingency contingency, Network networkClone, Crac crac, Set<PstRangeAction> rangeActionsToRegulate, RaoResult raoResult, RaoParameters raoParameters) {
        simulateContingencyAndAppyCurativeActions(contingency, networkClone, crac, raoResult);
        Set<PstRangeAction> pstsRangeActionsToShift = filterOutPstsInAbutment(rangeActionsToRegulate, contingency, networkClone);
        Map<PstRangeAction, Integer> initialTapPerPst = getInitialTapPerPst(pstsRangeActionsToShift, networkClone);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(networkClone, pstsRangeActionsToShift, getLoadFlowParameters(raoParameters));
        logPstRegulationResultsForContingencyScenario(contingency, initialTapPerPst, regulatedTapPerPst);
        // TODO: apply?
        return new PstRegulationResult(contingency, regulatedTapPerPst);
    }

    private static LoadFlowParameters getLoadFlowParameters(RaoParameters raoParameters) {
        return raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters() : new LoadFlowParameters();
    }

    private static Set<PstRangeAction> filterOutPstsInAbutment(Set<PstRangeAction> rangeActionsToRegulate, Contingency contingency, Network networkClone) {
        Set<PstRangeAction> pstsRangeActionsToShift = new HashSet<>();
        for (PstRangeAction pstRangeAction : rangeActionsToRegulate) {
            if (isPstInAbutment(pstRangeAction, networkClone)) {
                BUSINESS_LOGS.info("PST {} will not be regulated for contingency scenario {} as it is in abutment.", pstRangeAction.getNetworkElement().getId(), contingency.getId());
            } else {
                pstsRangeActionsToShift.add(pstRangeAction);
            }
        }
        return pstsRangeActionsToShift;
    }

    private static boolean isPstInAbutment(PstRangeAction pstRangeAction, Network networkClone) {
        PhaseTapChanger phaseTapChanger = networkClone.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger();
        int currentTapPosition = phaseTapChanger.getTapPosition();
        return phaseTapChanger.getHighTapPosition() == currentTapPosition || phaseTapChanger.getLowTapPosition() == currentTapPosition;
    }

    private static void simulateContingencyAndAppyCurativeActions(Contingency contingency, Network networkClone, Crac crac, RaoResult raoResult) {
        // simulate contingency
        contingency.toModification().apply(networkClone);

        // apply automatons
        if (crac.hasAutoInstant()) {
            applyOptimalRemedialActionsForState(networkClone, raoResult, crac.getState(contingency, crac.getInstant(InstantKind.AUTO)));
        }

        // apply optimal curative remedial actions
        crac.getInstants(InstantKind.CURATIVE).stream()
            .map(instant -> crac.getState(contingency, instant))
            .forEach(state -> applyOptimalRemedialActionsForState(networkClone, raoResult, state));
    }

    private static Map<PstRangeAction, Integer> getInitialTapPerPst(Set<PstRangeAction> rangeActionsToRegulate, Network networkClone) {
        return rangeActionsToRegulate.stream().collect(Collectors.toMap(Function.identity(), pstRangeAction -> networkClone.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger().getTapPosition()));
    }

    private static void logPstRegulationResultsForContingencyScenario(Contingency contingency,
                                                                      Map<PstRangeAction, Integer> initialTapPerPst,
                                                                      Map<PstRangeAction, Integer> regulatedTapPerPst) {
        List<PstRangeAction> sortedPstRangeActions = initialTapPerPst.keySet().stream().sorted().toList();
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
            BUSINESS_LOGS.info("PST regulation for contingency scenario %s: %s".formatted(contingency.getId(), String.join(", ", shiftDetails)));
        }
    }
}
