/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.deserializers.DeserializerNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class StateDeserializer {

    private StateDeserializer() { }

    static void deserialize(JsonParser jsonParser, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard State deserializer as it requires the simpleCrac to compare
        // Contingency ids and Instant ids ids with what is in the SimpleCrac

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String contingencyId = null;
            String instantId = null;

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(SIMPLE_STATE_TYPE)) {
                            throw new FaraoException(String.format("SimpleCrac cannot deserialize other states types than %s", SIMPLE_STATE_TYPE));
                        }
                        break;

                    case ID:
                        // the id should be the concatenation of the contingency id and state id
                        jsonParser.nextToken();
                        break;

                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case INSTANT:
                        instantId = jsonParser.nextTextValue();
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }
            //add state in Crac
            simpleCrac.addState(contingencyId, instantId);
        }
    }
}
