/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoTopoOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.COSTLY_MIN_MARGIN_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOAD_FLOW_AND_SENSITIVITY_COMPUTATION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.LOOP_FLOW_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MNEC_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MULTI_THREADING;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.OBJECTIVE_FUNCTION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PST_REGULATION_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RANGE_ACTIONS_OPTIMIZATION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RELATIVE_MARGINS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.SEARCH_TREE_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.SECOND_PREVENTIVE_RAO;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.TOPOLOGICAL_ACTIONS_OPTIMIZATION;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonOpenRaoSearchTreeParameters implements JsonRaoParameters.ExtensionSerializer<OpenRaoSearchTreeParameters> {
    @Override
    public void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(OBJECTIVE_FUNCTION, parameters.getObjectiveFunctionParameters());
        jsonGenerator.writeObjectField(RANGE_ACTIONS_OPTIMIZATION, parameters.getRangeActionsOptimizationParameters());
        jsonGenerator.writeObjectField(TOPOLOGICAL_ACTIONS_OPTIMIZATION, parameters.getTopoOptimizationParameters());
        jsonGenerator.writeObjectField(SECOND_PREVENTIVE_RAO, parameters.getSecondPreventiveRaoParameters());
        jsonGenerator.writeObjectField(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION, parameters.getLoadFlowAndSensitivityParameters());
        jsonGenerator.writeObjectField(MULTI_THREADING, parameters.getMultithreadingParameters());
        if (parameters.getMnecParameters().isPresent()) {
            jsonGenerator.writeObjectField(MNEC_PARAMETERS, parameters.getMnecParameters().get());
        }
        if (parameters.getRelativeMarginsParameters().isPresent()) {
            jsonGenerator.writeObjectField(RELATIVE_MARGINS, parameters.getRelativeMarginsParameters().get());
        }
        if (parameters.getLoopFlowParameters().isPresent()) {
            jsonGenerator.writeObjectField(LOOP_FLOW_PARAMETERS, parameters.getLoopFlowParameters().get());
        }
        if (parameters.getMinMarginsParameters().isPresent()) {
            jsonGenerator.writeObjectField(COSTLY_MIN_MARGIN_PARAMETERS, parameters.getMinMarginsParameters().get());
        }
        if (parameters.getPstRegulationParameters().isPresent()) {
            jsonGenerator.writeObjectField(PST_REGULATION_PARAMETERS, parameters.getPstRegulationParameters().get());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public OpenRaoSearchTreeParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(jsonParser, deserializationContext, ReportNode.NO_OP);
    }

    @Override
    public OpenRaoSearchTreeParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, ReportNode reportNode) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new OpenRaoSearchTreeParameters(reportNode), reportNode);
    }

    @Override
    public OpenRaoSearchTreeParameters deserializeAndUpdate(JsonParser parser, DeserializationContext deserializationContext, OpenRaoSearchTreeParameters parameters) throws IOException {
        return deserializeAndUpdate(parser, deserializationContext, parameters, ReportNode.NO_OP);
    }

    @Override
    public OpenRaoSearchTreeParameters deserializeAndUpdate(JsonParser parser,
                                                            DeserializationContext deserializationContext,
                                                            OpenRaoSearchTreeParameters parameters,
                                                            ReportNode reportNode) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.currentName()) {
                case OBJECTIVE_FUNCTION -> {
                    parser.nextToken();
                    parameters.setObjectiveFunctionParameters(deserializationContext.readValue(parser, SearchTreeRaoObjectiveFunctionParameters.class));
                }
                case RANGE_ACTIONS_OPTIMIZATION -> {
                    parser.nextToken();
                    parameters.setRangeActionsOptimizationParameters(deserializationContext.readValue(parser, SearchTreeRaoRangeActionsOptimizationParameters.class));
                }
                case TOPOLOGICAL_ACTIONS_OPTIMIZATION -> {
                    parser.nextToken();
                    parameters.setTopoOptimizationParameters(deserializationContext.readValue(parser, SearchTreeRaoTopoOptimizationParameters.class));
                }
                case MULTI_THREADING -> {
                    parser.nextToken();
                    parameters.setMultithreadingParameters(deserializationContext.readValue(parser, MultithreadingParameters.class));
                }
                case SECOND_PREVENTIVE_RAO -> {
                    parser.nextToken();
                    parameters.setSecondPreventiveRaoParameters(deserializationContext.readValue(parser, SecondPreventiveRaoParameters.class));
                }
                case LOAD_FLOW_AND_SENSITIVITY_COMPUTATION -> {
                    parser.nextToken();
                    parameters.setLoadFlowAndSensitivityParameters(deserializationContext.readValue(parser, LoadFlowAndSensitivityParameters.class));
                }
                case MNEC_PARAMETERS -> {
                    parser.nextToken();
                    parameters.setMnecParameters(deserializationContext.readValue(parser, SearchTreeRaoMnecParameters.class));
                }
                case RELATIVE_MARGINS -> {
                    parser.nextToken();
                    parameters.setRelativeMarginsParameters(deserializationContext.readValue(parser, SearchTreeRaoRelativeMarginsParameters.class));
                }
                case LOOP_FLOW_PARAMETERS -> {
                    parser.nextToken();
                    parameters.setLoopFlowParameters(deserializationContext.readValue(parser, SearchTreeRaoLoopFlowParameters.class));
                }
                case COSTLY_MIN_MARGIN_PARAMETERS -> {
                    parser.nextToken();
                    parameters.setMinMarginsParameters(deserializationContext.readValue(parser, SearchTreeRaoCostlyMinMarginParameters.class));
                }
                case PST_REGULATION_PARAMETERS -> {
                    parser.nextToken();
                    parameters.setPstRegulationParameters(deserializationContext.readValue(parser, SearchTreeRaoPstRegulationParameters.class));
                }
                default ->
                    throw new OpenRaoException("Unexpected field in open rao search tree parameters: " + parser.currentName());
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return SEARCH_TREE_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super OpenRaoSearchTreeParameters> getExtensionClass() {
        return OpenRaoSearchTreeParameters.class;
    }

}
