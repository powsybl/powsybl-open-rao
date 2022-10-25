/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.serializers.RangeActionResultsSerializationUtils.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class PstRangeActionResultArraySerializer {

    private PstRangeActionResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<PstRangeAction> sortedListOfRangeActions = crac.getPstRangeActions().stream()
                .sorted(Comparator.comparing(RangeAction::getId))
                .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(PSTRANGEACTION_RESULTS);
        for (PstRangeAction pstRangeAction : sortedListOfRangeActions) {
            serializeRangeActionResult(pstRangeAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeRangeActionResult(PstRangeAction pstRangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(PSTRANGEACTION_ID, pstRangeAction.getId());

        Integer initialTap = safeGetPreOptimizedTap(raoResult, crac.getPreventiveState(), pstRangeAction);
        Double initialSetpoint = safeGetPreOptimizedSetpoint(raoResult, crac.getPreventiveState(), pstRangeAction);

        if (initialTap != null) {
            jsonGenerator.writeNumberField(INITIAL_TAP, initialTap);
        }
        if (!Double.isNaN(initialSetpoint)) {
            jsonGenerator.writeNumberField(INITIAL_SETPOINT, initialSetpoint);
        }

        addAfterPraValuesForNonPreventivePsts(pstRangeAction, raoResult, crac, jsonGenerator);
        addAfterAraValuesForCurativePsts(pstRangeAction, raoResult, crac, jsonGenerator);

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, pstRangeAction))
                .sorted(STATE_COMPARATOR)
                .collect(Collectors.toList());

        Map<State, Pair<Integer, Double>> activatedTapsAndSetpoints = statesWhenRangeActionIsActivated.stream().collect(Collectors.toMap(
                Function.identity(), state -> Pair.of(safeGetOptimizedTap(raoResult, state, pstRangeAction), safeGetOptimizedSetpoint(raoResult, state, pstRangeAction))
        ));
        writeStateToTapAndSetpointArray(jsonGenerator, activatedTapsAndSetpoints, RaoResultJsonConstants.STATES_ACTIVATED);

        jsonGenerator.writeEndObject();
    }

    private static boolean safeIsActivatedDuringState(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        // isActivatedDuringState might throw an exception, for instance if the RAO was run on one state only, and the
        // state in argument of this method is not the same state.
        try {
            return raoResult.isActivatedDuringState(state, pstRangeAction);
        } catch (FaraoException e) {
            return false;
        }
    }

    private static Integer safeGetPreOptimizedTap(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        try {
            return raoResult.getPreOptimizationTapOnState(state, pstRangeAction);
        } catch (FaraoException e) {
            return null;
        }
    }

    private static Integer safeGetOptimizedTap(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        try {
            return raoResult.getOptimizedTapOnState(state, pstRangeAction);
        } catch (FaraoException e) {
            return null;
        }
    }

    /**
     * If range action is not preventive, it might have an associated preventive RA on the same network element
     * In this case, this method exports its post-pra tap and setpoint values
     */
    static void addAfterPraValuesForNonPreventivePsts(PstRangeAction pstRangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if ((isRangeActionCurative(pstRangeAction, crac) || isRangeActionAuto(pstRangeAction, crac)) && !isRangeActionPreventive(pstRangeAction, crac)) {
            PstRangeAction pra = getSimilarPstRangeActionAvailableAtOtherState(pstRangeAction, crac.getPreventiveState(), crac);
            if (pra != null) {
                Integer afterPraTap = safeGetOptimizedTap(raoResult, crac.getPreventiveState(), pra);
                Double afterPraSetpoint = safeGetOptimizedSetpoint(raoResult, crac.getPreventiveState(), pra);
                if (afterPraTap != null) {
                    jsonGenerator.writeNumberField(AFTER_PRA_TAP, afterPraTap);
                }
                if (!Double.isNaN(afterPraSetpoint)) {
                    jsonGenerator.writeNumberField(AFTER_PRA_SETPOINT, afterPraSetpoint);
                }
            }
        }
    }

    /**
     * If range action is curative, it might have an associated automatic RA on the same network element
     * In this case, this method exports its post-ara tap and setpoint values
     */
    static void addAfterAraValuesForCurativePsts(PstRangeAction pstRangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if (!isRangeActionCurative(pstRangeAction, crac) || isRangeActionAuto(pstRangeAction, crac)) {
            return;
        }
        Map<State, Pair<Integer, Double>> postAraTapsAndSetpoints = new HashMap<>();
        crac.getStates(Instant.AUTO).forEach(autoState -> {
                PstRangeAction ara = getSimilarPstRangeActionAvailableAtOtherState(pstRangeAction, autoState, crac);
                if (Objects.nonNull(ara)) {
                    postAraTapsAndSetpoints.put(autoState, Pair.of(safeGetOptimizedTap(raoResult, autoState, ara), safeGetOptimizedSetpoint(raoResult, autoState, ara)));
                }
            }
        );
        writeStateToTapAndSetpointArray(jsonGenerator, postAraTapsAndSetpoints, AFTER_ARA_TAPS_SETPOINTS);
    }

    static PstRangeAction getSimilarPstRangeActionAvailableAtOtherState(PstRangeAction pstRangeAction, State otherState, Crac crac) {
        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(otherState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        return rangeActionsForState.stream()
                .filter(PstRangeAction.class::isInstance)
                .filter(otherRangeAction -> !otherRangeAction.equals(pstRangeAction))
                .filter(otherRangeAction -> otherRangeAction.getNetworkElements().equals(pstRangeAction.getNetworkElements()))
                .map(PstRangeAction.class::cast)
                .findFirst().orElse(null);
    }
}
