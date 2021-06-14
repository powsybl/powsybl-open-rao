/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultSerializer extends AbstractJsonSerializer<RaoResult> {

    private Crac crac;

    RaoResultSerializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public void serialize(RaoResult raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField(COMPUTATION_STATUS, serializeStatus(raoResult.getComputationStatus()));
        serializeCostResults(raoResult, jsonGenerator);
        serializeFlowCnecResults(raoResult, jsonGenerator);
        serializeNetworkActionResults(raoResult, jsonGenerator);
        serializeRangeActionResults(raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();

    }

    private void serializeCostResults(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(COST_RESULTS);
        serializeCostResultForOptimizationState(OptimizationState.INITIAL, raoResult, jsonGenerator);
        serializeCostResultForOptimizationState(OptimizationState.AFTER_PRA, raoResult, jsonGenerator);
        serializeCostResultForOptimizationState(OptimizationState.AFTER_CRA, raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private void serializeCostResultForOptimizationState(OptimizationState optState, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));

        jsonGenerator.writeNumberField(FUNCTIONAL_COST, raoResult.getFunctionalCost(optState));

        if (!raoResult.getVirtualCostNames().isEmpty()) {
            jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);

            for (String virtualCostName : raoResult.getVirtualCostNames()) {
                jsonGenerator.writeNumberField(virtualCostName, raoResult.getVirtualCost(optState, virtualCostName));
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResults(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = crac.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(FLOWCNEC_RESULTS);
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            serializeFlowCnecResult(flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(FLOWCNEC_ID, flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(OptimizationState.INITIAL, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_PRA, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_CRA, flowCnec, raoResult, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResultForOptimizationState(OptimizationState optState, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, MEGAWATT, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, AMPERE, flowCnec, raoResult, jsonGenerator);
        double ptdfZonalSum = raoResult.getPtdfZonalSum(optState, flowCnec);
        if (!Double.isNaN(ptdfZonalSum)) {
            jsonGenerator.writeNumberField(ZONAL_PTDF_SUM, ptdfZonalSum);
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double flow = raoResult.getFlow(optState, flowCnec, unit);
        double margin = raoResult.getMargin(optState, flowCnec, unit);
        double relativeMargin = raoResult.getRelativeMargin(optState, flowCnec, unit);
        double loopFlow = raoResult.getLoopFlow(optState, flowCnec, unit);
        double commercialFlow = raoResult.getCommercialFlow(optState, flowCnec, unit);

        if (Double.isNaN(flow) && Double.isNaN(margin) && Double.isNaN(relativeMargin) && Double.isNaN(loopFlow) && Double.isNaN(commercialFlow)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField(FLOW, flow);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, margin);
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField(RELATIVE_MARGIN, relativeMargin);
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField(LOOP_FLOW, loopFlow);
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField(COMMERCIAL_FLOW, commercialFlow);
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeNetworkActionResults(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        List<NetworkAction> sortedListOfNetworkActions = crac.getNetworkActions().stream()
            .sorted(Comparator.comparing(NetworkAction::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(NETWORKACTION_RESULTS);
        for (NetworkAction networkAction : sortedListOfNetworkActions) {
            serializeNetworkActionResult(networkAction, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeNetworkActionResult(NetworkAction networkAction, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(NETWORKACTION_ID, networkAction.getId());

        List<State> statesWhenNetworkActionIsActivated = crac.getStates().stream()
            .filter(state -> raoResult.isActivatedDuringState(state, networkAction))
            .sorted(getStateComparatorForJson())
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(STATES_ACTIVATED_NETWORKACTION);
        for (State state: statesWhenNetworkActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(state.getInstant()));
            if (state.getContingency().isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, state.getContingency().get().getId());

            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

    private void serializeRangeActionResults(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        List<PstRangeAction> sortedListOfRangeActions = crac.getPstRangeActions().stream()
            .sorted(Comparator.comparing(RangeAction::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(PSTRANGEACTION_RESULTS);
        for (PstRangeAction pstRangeAction : sortedListOfRangeActions) {
            serializeRangeActionResult(pstRangeAction, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeRangeActionResult(PstRangeAction pstRangeAction, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(PSTRANGEACTION_ID, pstRangeAction.getId());
        jsonGenerator.writeStringField(PST_NETWORKELEMENT_ID, pstRangeAction.getNetworkElement().getId());

        double initialTap = raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pstRangeAction);
        double initialSetpoint = raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pstRangeAction);

        if (!Double.isNaN(initialTap)) {
            jsonGenerator.writeNumberField(INITIAL_TAP, initialTap);
        }
        if (!Double.isNaN(initialSetpoint)) {
            jsonGenerator.writeNumberField(INITIAL_SETPOINT, initialSetpoint);
        }

        List<State> statesWhenRangeActionIsActivated = crac.getStates().stream()
            .filter(state -> raoResult.isActivatedDuringState(state, pstRangeAction))
            .sorted(getStateComparatorForJson())
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(STATES_ACTIVATED_PSTRANGEACTION);
        for (State state: statesWhenRangeActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(state.getInstant()));

            int tap = raoResult.getOptimizedTapOnState(state, pstRangeAction);
            double setpoint = raoResult.getOptimizedSetPointOnState(state, pstRangeAction);

            if (state.getContingency().isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, state.getContingency().get().getId());
            }
            jsonGenerator.writeNumberField(TAP, tap);
            if (!Double.isNaN(setpoint)) {
                jsonGenerator.writeNumberField(SETPOINT, setpoint);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private static Comparator<State> getStateComparatorForJson() {
        return  (s1, s2) -> {
            if (s1.getInstant().getOrder() != s2.getInstant().getOrder()) {
                return s1.compareTo(s2);
            } else if (s1.getInstant().equals(Instant.PREVENTIVE)) {
                return 0;
            } else {
                return s1.getContingency().get().getId().compareTo(s2.getContingency().get().getId());
            }
        };
    }

}
