/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonResultVariantManager implements ExtensionsHandler.ExtensionSerializer<Crac, ResultVariantManager> {

    @Override
    public void serialize(ResultVariantManager resultVariantManager, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("variantIds", resultVariantManager.getVariants());
        jsonGenerator.writeEndObject();
    }

    @Override
    public ResultVariantManager deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ResultVariantManager resultVariantManager = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals("variantIds")) {
                jsonParser.nextToken();
                Set<String> variantIds = jsonParser.readValueAs(new TypeReference<Set<String>>() {
                });
                resultVariantManager = new ResultVariantManager(variantIds);
            } else {
                throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        return resultVariantManager;
    }

    @Override
    public String getExtensionName() {
        return "ResultVariantManager";
    }

    @Override
    public String getCategoryName() {
        return "identifiable";
    }

    @Override
    public Class<? super ResultVariantManager> getExtensionClass() {
        return ResultVariantManager.class;
    }
}
