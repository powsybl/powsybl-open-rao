/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraints.io;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.intertemporalconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.intertemporalconstraints.TimeCouplingConstraints;

import java.io.IOException;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCouplingConstraintsDeserializer extends StdDeserializer<TimeCouplingConstraints> {

    public TimeCouplingConstraintsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public TimeCouplingConstraints deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        TimeCouplingConstraints timeCouplingConstraints = new TimeCouplingConstraints();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case JsonTimeCouplingConstraints.TYPE, JsonTimeCouplingConstraints.VERSION -> jsonParser.nextToken();
                case JsonTimeCouplingConstraints.GENERATOR_CONSTRAINTS -> {
                    jsonParser.nextToken();
                    List.of(jsonParser.readValueAs(GeneratorConstraints[].class)).forEach(timeCouplingConstraints::addGeneratorConstraints);
                }
                default ->
                    throw new OpenRaoException("Unexpected field '%s' in JSON time-coupling constraints.".formatted(jsonParser.currentName()));
            }
        }
        return timeCouplingConstraints;
    }
}
