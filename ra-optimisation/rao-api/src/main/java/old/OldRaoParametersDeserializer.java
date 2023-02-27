/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package old;

import com.farao_community.farao.commons.FaraoException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OldRaoParametersDeserializer extends StdDeserializer<OldRaoParameters> {

    OldRaoParametersDeserializer() {
        super(OldRaoParameters.class);
    }

    @Override
    public OldRaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new OldRaoParameters());
    }

    @Override
    public OldRaoParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, OldRaoParameters parameters) throws IOException {

        List<Extension<OldRaoParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "version":
                    parser.nextToken();
                    break;
                case "objective-function":
                    parameters.setObjectiveFunction(stringToObjectiveFunction(parser.nextTextValue()));
                    break;
                case "max-number-of-iterations":
                    parser.nextToken();
                    parameters.setMaxIterations(parser.getIntValue());
                    break;
                case "pst-penalty-cost":
                    parser.nextToken();
                    parameters.setPstPenaltyCost(parser.getDoubleValue());
                    break;
                case "pst-sensitivity-threshold":
                    parser.nextToken();
                    parameters.setPstSensitivityThreshold(parser.getDoubleValue());
                    break;
                case "hvdc-penalty-cost":
                    parser.nextToken();
                    parameters.setHvdcPenaltyCost(parser.getDoubleValue());
                    break;
                case "hvdc-sensitivity-threshold":
                    parser.nextToken();
                    parameters.setHvdcSensitivityThreshold(parser.getDoubleValue());
                    break;
                case "injection-ra-penalty-cost":
                    parser.nextToken();
                    parameters.setInjectionRaPenaltyCost(parser.getDoubleValue());
                    break;
                case "injection-ra-sensitivity-threshold":
                    parser.nextToken();
                    parameters.setInjectionRaSensitivityThreshold(parser.getDoubleValue());
                    break;
                case "sensitivity-fallback-over-cost":
                    parser.nextToken();
                    parameters.setFallbackOverCost(parser.getDoubleValue());
                    break;
                case "rao-with-loop-flow-limitation":
                    parser.nextToken();
                    parameters.setRaoWithLoopFlowLimitation(parser.getBooleanValue());
                    break;
                case "loop-flow-acceptable-augmentation":
                    parser.nextToken();
                    parameters.setLoopFlowAcceptableAugmentation(parser.getDoubleValue());
                    break;
                case "loop-flow-approximation":
                    parameters.setLoopFlowApproximationLevel(stringToLoopFlowApproximationLevel(parser.nextTextValue()));
                    break;
                case "loop-flow-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setLoopFlowConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                case "loop-flow-violation-cost":
                    parser.nextToken();
                    parameters.setLoopFlowViolationCost(parser.getDoubleValue());
                    break;
                case "loop-flow-countries":
                    parser.nextToken();
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode arrayNode = objectMapper.readTree(parser);
                    List<String> countryStrings = objectMapper.readValue(arrayNode.traverse(), new TypeReference<ArrayList<String>>() { });
                    parameters.setLoopflowCountries(countryStrings);
                    break;
                case "rao-with-mnec-limitation":
                    parser.nextToken();
                    parameters.setRaoWithMnecLimitation(parser.getBooleanValue());
                    break;
                case "mnec-acceptable-margin-diminution":
                    parser.nextToken();
                    parameters.setMnecAcceptableMarginDiminution(parser.getDoubleValue());
                    break;
                case "mnec-violation-cost":
                    parser.nextToken();
                    parameters.setMnecViolationCost(parser.getDoubleValue());
                    break;
                case "mnec-constraint-adjustment-coefficient":
                    parser.nextToken();
                    parameters.setMnecConstraintAdjustmentCoefficient(parser.getDoubleValue());
                    break;
                case "relative-margin-ptdf-boundaries":
                    readRelativeMarginPtdfBoundaries(parser, parameters);
                    break;
                case "ptdf-sum-lower-bound":
                    parser.nextToken();
                    parameters.setPtdfSumLowerBound(parser.getDoubleValue());
                    break;
                case "perimeters-in-parallel":
                    parser.nextToken();
                    parameters.setPerimetersInParallel(parser.getIntValue());
                    break;
                case "optimization-solver":
                    parameters.setSolver(stringToSolver(parser.nextTextValue()));
                    break;
                case "relative-mip-gap":
                    parser.nextToken();
                    parameters.setRelativeMipGap(parser.getDoubleValue());
                    break;
                case "solver-specific-parameters":
                    parameters.setSolverSpecificParameters(parser.nextTextValue());
                    break;
                case "pst-optimization-approximation":
                    parameters.setPstOptimizationApproximation(stringToPstApproximation(parser.nextTextValue()));
                    break;
                case "load-flow-provider":
                    parameters.setLoadFlowProvider(parser.nextTextValue());
                    break;
                case "sensitivity-provider":
                    parameters.setSensitivityProvider(parser.nextTextValue());
                    break;
                case "sensitivity-parameters":
                    parser.nextToken();
                    parameters.setDefaultSensitivityAnalysisParameters(JsonSensitivityAnalysisParameters.createObjectMapper().readerForUpdating(parameters.getDefaultSensitivityAnalysisParameters()).readValue(parser));
                    break;
                case "fallback-sensitivity-parameters":
                    readFallbackSensitivityParameters(parser, parameters);
                    break;
                case "forbid-cost-increase":
                    parser.nextToken();
                    parameters.setForbidCostIncrease(parser.getBooleanValue());
                    break;
                case "extensions":
                    parser.nextToken();
                    if (parameters.getExtensions().isEmpty()) {
                        extensions = JsonUtil.readExtensions(parser, deserializationContext, OldJsonRaoParameters.getExtensionSerializers());
                    } else {
                        JsonUtil.updateExtensions(parser, deserializationContext, OldJsonRaoParameters.getExtensionSerializers(), parameters);
                    }
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + parser.getCurrentName());
            }
        }

        OldJsonRaoParameters.getExtensionSerializers().addExtensions(parameters, extensions);
        return parameters;
    }

    private void readFallbackSensitivityParameters(JsonParser parser, OldRaoParameters parameters) throws IOException {
        parser.nextToken();
        if (parameters.getFallbackSensitivityAnalysisParameters() == null) {
            parameters.setFallbackSensitivityAnalysisParameters(new SensitivityAnalysisParameters());
        }
        parameters.setFallbackSensitivityAnalysisParameters(JsonSensitivityAnalysisParameters.createObjectMapper().readerForUpdating(parameters.getFallbackSensitivityAnalysisParameters()).readValue(parser));
    }

    private void readRelativeMarginPtdfBoundaries(JsonParser parser, OldRaoParameters parameters) throws IOException {
        if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
            List<String> boundaries = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                boundaries.add(parser.getValueAsString());
            }
            parameters.setRelativeMarginPtdfBoundariesFromString(boundaries);
        }
    }

    private OldRaoParameters.ObjectiveFunction stringToObjectiveFunction(String string) {
        try {
            return OldRaoParameters.ObjectiveFunction.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown objective function value: %s", string));
        }
    }

    private OldRaoParameters.LoopFlowApproximationLevel stringToLoopFlowApproximationLevel(String string) {
        try {
            return OldRaoParameters.LoopFlowApproximationLevel.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown loopflow approximation level: %s", string));
        }
    }

    private OldRaoParameters.Solver stringToSolver(String string) {
        try {
            return OldRaoParameters.Solver.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown solver: %s", string));
        }
    }

    private OldRaoParameters.PstOptimizationApproximation stringToPstApproximation(String string) {
        try {
            return OldRaoParameters.PstOptimizationApproximation.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown pst approximation: %s", string));
        }
    }
}