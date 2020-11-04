/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultDeserializer extends StdDeserializer<RaoResult> {

    RaoResultDeserializer() {
        super(RaoResult.class);
    }

    @Override
    public RaoResult deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new RaoResult(RaoResult.Status.UNDEFINED));
    }

    @Override
    public RaoResult deserialize(JsonParser parser, DeserializationContext deserializationContext, RaoResult raoResult) throws IOException {

        List<Extension<RaoResult>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {

                case "status":
                    parser.nextToken();
                    raoResult.setStatus(getStatusFromString(parser.getValueAsString()));
                    break;

                case "preOptimVariantId":
                    parser.nextToken();
                    raoResult.setPreOptimVariantId(parser.getValueAsString());
                    break;

                case "postOptimVariantIdPerStateId":
                    parser.nextToken();
                    raoResult.setPostOptimVariantIdPerStateId(parser.readValueAs(new TypeReference<Map<String, String>>() {
                    }));
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

    private RaoResult.Status getStatusFromString(String status) {
        switch (status) {

            case "FAILURE":
                return RaoResult.Status.FAILURE;

            case "SUCCESS":
                return RaoResult.Status.SUCCESS;

            case "UNDEFINED":
                return RaoResult.Status.UNDEFINED;

            default:
                throw new FaraoException("Unexpected field: " + status);
        }
    }
}
