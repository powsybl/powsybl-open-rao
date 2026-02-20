/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;

import java.io.IOException;
import java.util.List;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TimeCoupledConstraintsDeserializer extends StdDeserializer<TimeCoupledConstraints> {

    public TimeCoupledConstraintsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public TimeCoupledConstraints deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case JsonTimeCoupledConstraints.TYPE, JsonTimeCoupledConstraints.VERSION -> jsonParser.nextToken();
                case JsonTimeCoupledConstraints.GENERATOR_CONSTRAINTS -> {
                    jsonParser.nextToken();
                    List.of(jsonParser.readValueAs(GeneratorConstraints[].class)).forEach(timeCoupledConstraints::addGeneratorConstraints);
                }
                default ->
                    throw new OpenRaoException("Unexpected field '%s' in JSON time-coupled constraints.".formatted(jsonParser.currentName()));
            }
        }
        return timeCoupledConstraints;
    }
}
