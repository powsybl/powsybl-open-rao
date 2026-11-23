/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPreSolveParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.CURATIVE_SOLUTION_USAGE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_CURATIVE_SEARCH_TREE_DEPTH;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MAX_PREVENTIVE_SEARCH_TREE_DEPTH;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PREVENTIVE_SOLUTION_USAGE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PRE_SOLVE_PARAMETERS;

/**
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
public final class JsonPreSolveParameters {

    private JsonPreSolveParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<SearchTreeRaoPreSolveParameters> optionalPreSolveParameters = parameters.getPreSolveParameters();
        if (optionalPreSolveParameters.isPresent()) {
            SearchTreeRaoPreSolveParameters preSolveParameters = optionalPreSolveParameters.get();
            jsonGenerator.writeObjectFieldStart(PRE_SOLVE_PARAMETERS);
            jsonGenerator.writeNumberField(MAX_PREVENTIVE_SEARCH_TREE_DEPTH, preSolveParameters.getMaxPreventiveSearchTreeDepth());
            jsonGenerator.writeNumberField(MAX_CURATIVE_SEARCH_TREE_DEPTH, preSolveParameters.getMaxCurativeSearchTreeDepth());
            jsonGenerator.writeObjectField(PREVENTIVE_SOLUTION_USAGE, preSolveParameters.getPreventiveSolutionUsage());
            jsonGenerator.writeObjectField(CURATIVE_SOLUTION_USAGE, preSolveParameters.getCurativeSolutionUsage());
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters, ReportNode reportNode) throws IOException {
        SearchTreeRaoPreSolveParameters preSolveParameters = new SearchTreeRaoPreSolveParameters(reportNode);
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case MAX_PREVENTIVE_SEARCH_TREE_DEPTH -> {
                    jsonParser.nextToken();
                    preSolveParameters.setMaxPreventiveSearchTreeDepth(jsonParser.getIntValue());
                }
                case MAX_CURATIVE_SEARCH_TREE_DEPTH -> {
                    jsonParser.nextToken();
                    preSolveParameters.setMaxCurativeSearchTreeDepth(jsonParser.getIntValue());
                }
                case PREVENTIVE_SOLUTION_USAGE ->
                    preSolveParameters.setPreventiveSolutionUsage(stringToSolutionUsage(jsonParser.nextTextValue()));
                case CURATIVE_SOLUTION_USAGE ->
                    preSolveParameters.setCurativeSolutionUsage(stringToSolutionUsage(jsonParser.nextTextValue()));
                default -> throw new OpenRaoException(String.format("Cannot deserialize pre-solve parameters: unexpected field in %s (%s)", PRE_SOLVE_PARAMETERS, jsonParser.currentName()));
            }
        }
        searchTreeParameters.setPreSolveParameters(preSolveParameters);
    }

    private static SearchTreeRaoPreSolveParameters.SolutionUsage stringToSolutionUsage(String string) {
        try {
            return SearchTreeRaoPreSolveParameters.SolutionUsage.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown solution usage: %s", string));
        }
    }
}
