/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.FLOWCNEC_RESULTS;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultDeserializer extends JsonDeserializer<RaoResult> {

    private Crac crac;

    private RaoResultDeserializer() {
    }

    public RaoResultDeserializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public RaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        RaoResultImpl raoResult = new RaoResultImpl();

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case FLOWCNEC_RESULTS:
                    jsonParser.nextToken();
                    FlowCnecResultArrayDeserializer.deserialize(jsonParser, raoResult, crac);
                    break;

                default:
                    throw new FaraoException("Unexpected field in RaoResult: " + jsonParser.getCurrentName());
            }
        }
        return raoResult;
    }
}

