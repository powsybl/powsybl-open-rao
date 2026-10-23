/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.openrao.data.timecoupledconstraints.PstConstraints;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Atena Amnache {@literal <atena.amnache at rte-france.com>}
 */
public class PstConstraintsSerializer extends StdSerializer<PstConstraints> {

    public PstConstraintsSerializer(Class<PstConstraints> t) {
        super(t);
    }

    @Override
    public void serialize(PstConstraints pstConstraints, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField(JsonTimeCoupledConstraints.PST_ID, pstConstraints.getPstId());

        Optional<Integer> upwardTapGradient = pstConstraints.getUpwardTapGradient();
        if (upwardTapGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCoupledConstraints.UPWARD_TAP_GRADIENT, upwardTapGradient.get());
        }

        Optional<Integer> downwardTapGradient = pstConstraints.getDownwardTapGradient();
        if (downwardTapGradient.isPresent()) {
            jsonGenerator.writeNumberField(JsonTimeCoupledConstraints.DOWNWARD_TAP_GRADIENT, downwardTapGradient.get());
        }

        jsonGenerator.writeEndObject();
    }
}
