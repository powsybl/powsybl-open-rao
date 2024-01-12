/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonCsaCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<CsaCracCreationParameters> {

    private static final String USE_CNEC_GEOGRAPHICAL_FILTER = "use-cnec-geographical-filter";

    @Override
    public void serialize(CsaCracCreationParameters csaParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeUseCnecGeographicalFilter(csaParameters.getUseCnecGeographicalFilter(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public CsaCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CsaCracCreationParameters());
    }

    @Override
    public CsaCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CsaCracCreationParameters parameters) throws IOException {
        boolean cnecGeogrphicalFilterAlreadyMet = false;
        while (!jsonParser.nextToken().isStructEnd()) {
            if (USE_CNEC_GEOGRAPHICAL_FILTER.equals(jsonParser.getCurrentName())) {
                if (cnecGeogrphicalFilterAlreadyMet) {
                    throw new OpenRaoException("Duplicated field: " + USE_CNEC_GEOGRAPHICAL_FILTER);
                }
                jsonParser.nextToken();
                parameters.setUseCnecGeographicalFilter(jsonParser.readValueAs(Boolean.class));
                cnecGeogrphicalFilterAlreadyMet = true;
            } else {
                throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CsaCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CsaCracCreationParameters> getExtensionClass() {
        return CsaCracCreationParameters.class;
    }

    private void serializeUseCnecGeographicalFilter(boolean useCnecGeographicalFilter, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStringField(USE_CNEC_GEOGRAPHICAL_FILTER, ((Boolean) useCnecGeographicalFilter).toString());
    }
}
