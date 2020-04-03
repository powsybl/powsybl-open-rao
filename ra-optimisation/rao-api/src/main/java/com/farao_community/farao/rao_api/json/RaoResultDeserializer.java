/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultDeserializer extends StdDeserializer<RaoResult> {

    RaoResultDeserializer() {
        super(RaoResult.class);
    }

    @Override
    public RaoResult deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new RaoResult());//TODO
    }

    @Override
    public RaoResult deserialize(JsonParser parser, DeserializationContext deserializationContext, RaoResult raoResult) throws IOException {

        List<Extension<RaoResult>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {

                case "status":
                    parser.nextToken();
                    raoResult.setStatus(parser.getValueAsString().equals("")); //TODO
                    break;

                case "preOptimVariantId":
                    parser.nextToken();
                    raoResult.setPreOptimVariantId(parser.getValueAsString());
                    break;

                case "postOptimVariantId":
                    parser.nextToken();
                    raoResult.setPostOptimVariantId(parser.getValueAsString());
                    break;

                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonRaoResult.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonRaoResult.getExtensionSerializers().addExtensions(raoResult, extensions);

        return raoResult;
    }

}
