/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

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
        jsonGenerator.writeStringField(PST_NETWORKELEMENT_ID, pstRangeAction.getNetworkElement().getId());

        Integer initialTap = safeGetPreOptimizedTap(raoResult, crac.getPreventiveState(), pstRangeAction);
        Double initialSetpoint = safeGetPreOptimizedSetpoint(raoResult, crac.getPreventiveState(), pstRangeAction);

        if (initialTap != null) {
            jsonGenerator.writeNumberField(INITIAL_TAP, initialTap);
        }
        if (!Double.isNaN(initialSetpoint)) {
            jsonGenerator.writeNumberField(INITIAL_SETPOINT, initialSetpoint);
        }

        addAfterPraValuesForPurelyCurativePsts(pstRangeAction, raoResult, crac, jsonGenerator);

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, pstRangeAction))
                .sorted(STATE_COMPARATOR)
                .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(STATES_ACTIVATED_PSTRANGEACTION);
        for (State state : statesWhenRangeActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(state.getInstant()));

            Optional<Contingency> optContingency = state.getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
            }

            Integer tap = safeGetOptimizedTap(raoResult, state, pstRangeAction);
            Double setpoint = safeGetOptimizedSetpoint(raoResult, state, pstRangeAction);

            if (tap != null) {
                jsonGenerator.writeNumberField(TAP, tap);
            }
            if (!Double.isNaN(setpoint)) {
                jsonGenerator.writeNumberField(SETPOINT, setpoint);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private static boolean safeIsActivatedDuringState(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        // isActivatedDuringState might throw an exception, for instance if the RAO was run one one state only, and the
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

    private static Double safeGetPreOptimizedSetpoint(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        try {
            return raoResult.getPreOptimizationSetPointOnState(state, pstRangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static Double safeGetOptimizedSetpoint(RaoResult raoResult, State state, PstRangeAction pstRangeAction) {
        try {
            return raoResult.getOptimizedSetPointOnState(state, pstRangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    /**
     * If range action is purely curative, it might have an associated preventive RA on the same network element
     * In this case, this method exports its post-pra tap and setpoint values
     */
    static void addAfterPraValuesForPurelyCurativePsts(PstRangeAction pstRangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if (isRangeActionCurative(pstRangeAction, crac) && !isRangeActionPreventive(pstRangeAction, crac)) {
            PstRangeAction pra = getPreventivePstRangeActionAssociated(pstRangeAction, crac);
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

    static boolean isRangeActionPreventive(RangeAction rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionCurative(RangeAction rangeAction, Crac crac) {
        return crac.getStates().stream()
                .filter(state -> !state.equals(crac.getPreventiveState()))
                .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    static boolean isRangeActionAvailableInState(RangeAction rangeAction, State state, Crac crac) {
        Set<RangeAction> rangeActionsForState = crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        return rangeActionsForState.contains(rangeAction);
    }

    static PstRangeAction getPreventivePstRangeActionAssociated(PstRangeAction pstRangeAction, Crac crac) {
        Set<RangeAction> rangeActionsForState = crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        return rangeActionsForState.stream()
                .filter(PstRangeAction.class::isInstance)
                .filter(otherRangeAction -> !otherRangeAction.equals(pstRangeAction))
                .filter(otherRangeAction -> otherRangeAction.getNetworkElements().equals(pstRangeAction.getNetworkElements()))
                .map(PstRangeAction.class::cast)
                .findFirst().orElse(null);
    }
}
