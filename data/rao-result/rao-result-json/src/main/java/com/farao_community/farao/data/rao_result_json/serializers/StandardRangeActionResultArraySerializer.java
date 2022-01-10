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
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class StandardRangeActionResultArraySerializer {

    private StandardRangeActionResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<StandardRangeAction> sortedListOfRangeActions = crac.getRangeActions().stream()
                .filter(StandardRangeAction.class::isInstance)
                .map(StandardRangeAction.class::cast)
                .sorted(Comparator.comparing(RangeAction::getId))
                .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(STANDARDRANGEACTION_RESULTS);
        for (StandardRangeAction rangeAction : sortedListOfRangeActions) {
            serializeRangeActionResult(rangeAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeRangeActionResult(StandardRangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(RANGEACTION_ID, rangeAction.getId());

        Double initialSetpoint = safeGetPreOptimizedSetpoint(raoResult, crac.getPreventiveState(), rangeAction);

        if (!Double.isNaN(initialSetpoint)) {
            jsonGenerator.writeNumberField(INITIAL_SETPOINT, initialSetpoint);
        }

        // TODO : should we also do this for AUTO RAs if they exist in curative too?
        addAfterPraValuesForPurelyCurativeRas(rangeAction, raoResult, crac, jsonGenerator);

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, rangeAction))
                .sorted(STATE_COMPARATOR)
                .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(RaoResultJsonConstants.STATES_ACTIVATED);
        for (State state : statesWhenRangeActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(state.getInstant()));

            Optional<Contingency> optContingency = state.getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
            }

            Double setpoint = safeGetOptimizedSetpoint(raoResult, state, rangeAction);

            if (!Double.isNaN(setpoint)) {
                jsonGenerator.writeNumberField(SETPOINT, setpoint);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private static boolean safeIsActivatedDuringState(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        // isActivatedDuringState might throw an exception, for instance if the RAO was run one one state only, and the
        // state in argument of this method is not the same state.
        try {
            return raoResult.isActivatedDuringState(state, rangeAction);
        } catch (FaraoException e) {
            return false;
        }
    }

    private static Double safeGetPreOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        try {
            return raoResult.getPreOptimizationSetPointOnState(state, rangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static Double safeGetOptimizedSetpoint(RaoResult raoResult, State state, RangeAction<?> rangeAction) {
        try {
            return raoResult.getOptimizedSetPointOnState(state, rangeAction);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    /**
     * If range action is purely curative, it might have an associated preventive RA on the same network element
     * In this case, this method exports its post-pra setpoint values
     */
    static void addAfterPraValuesForPurelyCurativeRas(StandardRangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if (isRangeActionCurative(rangeAction, crac) && !isRangeActionPreventive(rangeAction, crac)) {
            StandardRangeAction<?> pra = getSimilarPreventiveRangeAction(rangeAction, crac);
            if (pra != null) {
                Double afterPraSetpoint = safeGetOptimizedSetpoint(raoResult, crac.getPreventiveState(), pra);
                if (!Double.isNaN(afterPraSetpoint)) {
                    jsonGenerator.writeNumberField(AFTER_PRA_SETPOINT, afterPraSetpoint);
                }
            }
        }
    }

    static boolean isRangeActionPreventive(RangeAction<?> rangeAction, Crac crac) {
        return isRangeActionAvailableInState(rangeAction, crac.getPreventiveState(), crac);
    }

    static boolean isRangeActionCurative(RangeAction<?> rangeAction, Crac crac) {
        return crac.getStates().stream()
                .filter(state -> !state.equals(crac.getPreventiveState()))
                .anyMatch(state -> isRangeActionAvailableInState(rangeAction, state, crac));
    }

    static boolean isRangeActionAvailableInState(RangeAction<?> rangeAction, State state, Crac crac) {
        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);
        return rangeActionsForState.contains(rangeAction);
    }

    static StandardRangeAction getSimilarPreventiveRangeAction(StandardRangeAction<?> rangeAction, Crac crac) {

        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(crac.getPreventiveState(), UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);

        if (rangeAction instanceof HvdcRangeAction) {
            return rangeActionsForState.stream()
                    .filter(HvdcRangeAction.class::isInstance)
                    .filter(otherRangeAction -> !otherRangeAction.equals(rangeAction))
                    .map(HvdcRangeAction.class::cast)
                    .filter(otherRangeAction -> otherRangeAction.getNetworkElement().equals(((HvdcRangeAction) rangeAction).getNetworkElement()))
                    .findFirst().orElse(null);

        } else if (rangeAction instanceof InjectionRangeAction) {

            return rangeActionsForState.stream()
                    .filter(InjectionRangeAction.class::isInstance)
                    .filter(otherRangeAction -> !otherRangeAction.equals(rangeAction))
                    .map(InjectionRangeAction.class::cast)
                    .filter(otherRangeAction -> hasSameNetworkElementAndKeys((InjectionRangeAction) rangeAction, otherRangeAction))
                    .findFirst().orElse(null);
        }

        return null;
    }

    private static boolean hasSameNetworkElementAndKeys(InjectionRangeAction rangeAction, InjectionRangeAction otherRangeAction) {

        if (rangeAction.getInjectionDistributionKeys().size() != otherRangeAction.getInjectionDistributionKeys().size()) {
            return false;
        }
        return rangeAction.getInjectionDistributionKeys().entrySet().stream()
                .allMatch(e -> Math.abs(otherRangeAction.getInjectionDistributionKeys().getOrDefault(e.getKey(), 0.0) - e.getValue()) < 1e-6);
    }
}
