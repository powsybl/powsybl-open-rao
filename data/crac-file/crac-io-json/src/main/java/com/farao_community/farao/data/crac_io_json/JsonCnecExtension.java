/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.CnecExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
    @AutoService(Cnec.ExtensionSerializer.class)
    public class JsonCnecExtension implements  Cnec.ExtensionSerializer<CnecExtension> {

    @Override
    public void serialize(CnecExtension cnecExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("flow-before-optim", cnecExtension.getFlowBeforeOptim());
        jsonGenerator.writeObjectField("flow-after-optim", cnecExtension.getFlowAfterOptim());
        jsonGenerator.writeEndObject();

    }

    @Override
    public CnecExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        CnecExtension ret = new CnecExtension(0, 0);

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "flow-before-optim":
                    jsonParser.nextToken();
                    ret.setFlowBeforeOptim(jsonParser.getDoubleValue());
                    break;
                case "flow-after-optim":
                    jsonParser.nextToken();
                    ret.setFlowAfterOptim(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        return ret;
    }

    @Override
    public String getExtensionName() {
        return "CnecExtension";
    }

    @Override
    public String getCategoryName() {
        return "cnec";
    }

    @Override
    public Class<? super CnecExtension> getExtensionClass() {
        return CnecExtension.class;
    }
}
