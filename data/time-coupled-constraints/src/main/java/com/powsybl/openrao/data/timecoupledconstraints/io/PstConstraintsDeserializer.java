/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints.io;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.timecoupledconstraints.PstConstraints;

import java.io.IOException;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public class PstConstraintsDeserializer extends StdDeserializer<PstConstraints> {

    public PstConstraintsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PstConstraints deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        PstConstraints.PstConstraintsBuilder builder = PstConstraints.create();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case JsonTimeCoupledConstraints.PST_ID -> {
                    jsonParser.nextToken();
                    builder.withPstId(jsonParser.getValueAsString());
                }
                case JsonTimeCoupledConstraints.UPWARD_TAP_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withUpwardTapGradient(jsonParser.getValueAsInt());
                }
                case JsonTimeCoupledConstraints.DOWNWARD_TAP_GRADIENT -> {
                    jsonParser.nextToken();
                    builder.withDownwardTapGradient(jsonParser.getValueAsInt());
                }
                default ->
                    throw new OpenRaoException("Unexpected field '%s' in JSON pst constraints.".formatted(jsonParser.currentName()));
            }
        }
        return builder.build();
    }
}
