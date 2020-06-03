/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.LinearRaoResult.SystematicSensitivityAnalysisStatus;
import com.farao_community.farao.rao_api.json.JsonRaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(JsonRaoResult.ExtensionSerializer.class)
public class JsonLinearRaoResult implements JsonRaoResult.ExtensionSerializer<LinearRaoResult>  {

    @Override
    public void serialize(LinearRaoResult resultExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("systematicSensitivityAnalysisParameters", resultExtension.getSystematicSensitivityAnalysisStatus());
        jsonGenerator.writeEndObject();
    }

    @Override
    public LinearRaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LinearRaoResult resultExtension = new LinearRaoResult();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "systematicSensitivityAnalysisParameters":
                    jsonParser.nextToken();
                    resultExtension.setSystematicSensitivityAnalysisStatus(getSystematicSensitivityAnalysisParametersFromString(jsonParser.getValueAsString()));
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return resultExtension;
    }

    @Override
    public String getExtensionName() {
        return "LinearRaoResult";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super LinearRaoResult> getExtensionClass() {
        return LinearRaoResult.class;
    }

    private SystematicSensitivityAnalysisStatus getSystematicSensitivityAnalysisParametersFromString(String systematicSensitivityAnalysisParameters) {
        switch (systematicSensitivityAnalysisParameters) {

            case "DEFAULT":
                return SystematicSensitivityAnalysisStatus.DEFAULT;

            case "FALLBACK":
                return SystematicSensitivityAnalysisStatus.FALLBACK;

            case "FAILURE":
                return SystematicSensitivityAnalysisStatus.FAILURE;

            default:
                throw new FaraoException("Unexpected field: " + systematicSensitivityAnalysisParameters);
        }
    }
}
