/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_io_json.deserializers;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.RemedialActionAdder;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnContingencyStateAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.open_rao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class OnStateArrayDeserializer {
    private OnStateArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnContingencyStateAdder<?> adder = ownerAdder.newOnContingencyStateUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        adder.withInstant(jsonParser.nextTextValue());
                        break;
                    case USAGE_METHOD:
                        adder.withUsageMethod(deserializeUsageMethod(jsonParser.nextTextValue()));
                        break;
                    case CONTINGENCY_ID:
                        adder.withContingency(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnContingencyState: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }

}
