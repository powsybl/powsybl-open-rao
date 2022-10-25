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
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeAction;
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
        for (StandardRangeAction<?> rangeAction : sortedListOfRangeActions) {
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

        addAfterPraValuesForNonPreventiveRas(rangeAction, raoResult, crac, jsonGenerator);
        addAfterAraValuesForCurativeRas(rangeAction, raoResult, crac, jsonGenerator);

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, rangeAction))
                .sorted(STATE_COMPARATOR)
                .collect(Collectors.toList());

        Map<State, Pair<Integer, Double>> activatedSetpoints = statesWhenRangeActionIsActivated.stream().collect(Collectors.toMap(
                Function.identity(), state -> Pair.of(null, safeGetOptimizedSetpoint(raoResult, state, rangeAction))
        ));
        writeStateToTapAndSetpointArray(jsonGenerator, activatedSetpoints, RaoResultJsonConstants.STATES_ACTIVATED);

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

    /**
     * If range action is not preventive, it might have an associated preventive RA on the same network element
     * In this case, this method exports its post-pra setpoint values
     */
    static void addAfterPraValuesForNonPreventiveRas(StandardRangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if ((isRangeActionAuto(rangeAction, crac) || isRangeActionCurative(rangeAction, crac)) && !isRangeActionPreventive(rangeAction, crac)) {
            StandardRangeAction<?> pra = getSimilarRangeActionAvailableAtOtherState(rangeAction, crac.getPreventiveState(), crac);
            if (pra != null) {
                Double afterPraSetpoint = safeGetOptimizedSetpoint(raoResult, crac.getPreventiveState(), pra);
                if (!Double.isNaN(afterPraSetpoint)) {
                    jsonGenerator.writeNumberField(AFTER_PRA_SETPOINT, afterPraSetpoint);
                }
            }
        }
    }

    /**
     * If range action is curative, it might have an associated automatic RA on the same network element
     * In this case, this method exports its post-ara setpoint values
     */
    static void addAfterAraValuesForCurativeRas(StandardRangeAction<?> rangeAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        if (!isRangeActionCurative(rangeAction, crac) || isRangeActionAuto(rangeAction, crac)) {
            return;
        }
        Map<State, Pair<Integer, Double>> postAraSetpoints = new HashMap<>();
        crac.getStates(Instant.AUTO).forEach(
            autoState -> {
                StandardRangeAction<?> ara = getSimilarRangeActionAvailableAtOtherState(rangeAction, autoState, crac);
                if (Objects.nonNull(ara)) {
                    postAraSetpoints.put(autoState, Pair.of(null, safeGetOptimizedSetpoint(raoResult, autoState, ara)));
                }
            });
        writeStateToTapAndSetpointArray(jsonGenerator, postAraSetpoints, AFTER_ARA_SETPOINTS);
    }

    static StandardRangeAction<?> getSimilarRangeActionAvailableAtOtherState(StandardRangeAction<?> rangeAction, State otherState, Crac crac) {
        Set<RangeAction<?>> rangeActionsForState = crac.getRangeActions(otherState, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED);

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
        } else {
            throw new FaraoException(String.format("RangeAction of class %s is not handled by RaoResult serializer", rangeAction.getClass()));
        }
    }

    private static boolean hasSameNetworkElementAndKeys(InjectionRangeAction rangeAction, InjectionRangeAction otherRangeAction) {
        if (!rangeAction.getInjectionDistributionKeys().keySet().equals(otherRangeAction.getInjectionDistributionKeys().keySet())) {
            return false;
        }
        return rangeAction.getInjectionDistributionKeys().entrySet().stream()
                .allMatch(e -> Math.abs(otherRangeAction.getInjectionDistributionKeys().getOrDefault(e.getKey(), 0.0) - e.getValue()) < 1e-6);
    }
}
