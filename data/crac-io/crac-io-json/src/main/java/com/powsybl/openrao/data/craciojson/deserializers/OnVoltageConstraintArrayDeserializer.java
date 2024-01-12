/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.OnVoltageConstraintAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public final class OnVoltageConstraintArrayDeserializer {
    private OnVoltageConstraintArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, RemedialActionAdder<?> ownerAdder, String version) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            OnVoltageConstraintAdder<?> adder = ownerAdder.newOnVoltageConstraintUsageRule();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        String instantId = jsonParser.nextTextValue();
                        adder.withInstant(instantId);
                        if (getPrimaryVersionNumber(version) < 2) {
                            adder.withUsageMethod(deseralizeInstantKind(instantId).equals(InstantKind.AUTO) ? UsageMethod.FORCED : UsageMethod.AVAILABLE);
                        }
                        break;
                    case USAGE_METHOD:
                        adder.withUsageMethod(deserializeUsageMethod(jsonParser.nextTextValue()));
                        break;
                    case VOLTAGE_CNEC_ID:
                        adder.withVoltageCnec(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in OnVoltageConstraint: " + jsonParser.getCurrentName());
                }
            }
            adder.add();
        }
    }
}
