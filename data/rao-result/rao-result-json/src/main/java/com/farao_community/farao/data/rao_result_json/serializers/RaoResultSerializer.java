package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
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

public class RaoResultSerializer extends AbstractJsonSerializer<RaoResult> {

    @Override
    public void serialize(RaoResult raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        serializeFlowCnecsResult(raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();

    }

    private void serializeFlowCnecsResult(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        List<FlowCnec> sortedListOfFlowCnecs = raoResult.getFlowCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart("flowCnecResults");
        for (FlowCnec flowCnec : sortedListOfFlowCnecs) {
            serializeFlowCnecResult(flowCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeFlowCnecResult(FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("flowCnecId", flowCnec.getId());

        serializeFlowCnecResultForOptimizationState(OptimizationState.INITIAL, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_PRA, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationState(OptimizationState.AFTER_CRA, flowCnec, raoResult, jsonGenerator);

        jsonGenerator.writeEndObject();
    }

    private void serializeFlowCnecResultForOptimizationState(OptimizationState optState, FlowCnec flowCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        //todo use constant instead of optState.toString()
        jsonGenerator.writeObjectFieldStart(optState.toString());
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, MEGAWATT, flowCnec, raoResult, jsonGenerator);
        serializeFlowCnecResultForOptimizationStateAndUnit(optState, AMPERE, flowCnec, raoResult, jsonGenerator);

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

        //todo use constant instead of unit.toString()
        jsonGenerator.writeObjectFieldStart(unit.toString());
        if (!Double.isNaN(flow)) {
            jsonGenerator.writeNumberField("flow", flow);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField("margin", margin);
        }
        if (!Double.isNaN(relativeMargin)) {
            jsonGenerator.writeNumberField("relativeMargin", relativeMargin);
        }
        if (!Double.isNaN(loopFlow)) {
            jsonGenerator.writeNumberField("loopFlow", loopFlow);
        }
        if (!Double.isNaN(commercialFlow)) {
            jsonGenerator.writeNumberField("commercialFlow", commercialFlow);
        }
        jsonGenerator.writeEndObject();
    }
}
